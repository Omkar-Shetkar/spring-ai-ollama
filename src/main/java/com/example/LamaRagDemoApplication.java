package com.example;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.autoconfigure.ollama.OllamaChatProperties;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class LamaRagDemoApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(LamaRagDemoApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(LamaRagDemoApplication.class, args);
    }

    private static final String directoryPath = "src/main/resources/docs";

    @Bean
    ApplicationRunner applicationRunner(
            JdbcTemplate jdbcTemplate,
            VectorStore vectorStore
            // ,@Value("${documents.directory.path}") String directoryPath
    ) {
        return args -> {
            Path directory = Paths.get(directoryPath);
            try (var paths = Files.list(directory)) {
                // Extract the text from each PDF document
                paths.filter(Files::isRegularFile)
                        .forEach(path -> {
                            try {
                                Resource resource = new UrlResource(path.toUri());
                                LOGGER.info("Processing document: {}", resource.getFilename());
                                PagePdfDocumentReader pdfReader = new PagePdfDocumentReader("classpath:/docs/story.pdf",
                                        PdfDocumentReaderConfig.builder()
                                                .withPageTopMargin(0)
                                                .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                                        .withNumberOfBottomTextLinesToDelete(0)
                                                        .build())
                                                .withPagesPerDocument(1)
                                                .build());

                                // Transform the PDF document into a list of vectors
                                var textSplitter = new TokenTextSplitter();
                                var docs = textSplitter.apply(pdfReader.get());

                                // Load the vectors into the vector store
                                LOGGER.info("Loading PDF: {}", resource.getFilename());
                                vectorStore.accept(docs);
                                LOGGER.info("Successfully loaded PDF: {}", resource.getFilename());

                            } catch (MalformedURLException ex) {
                                throw new RuntimeException(ex);
                            }
                        });
            }
        };
    }

}
