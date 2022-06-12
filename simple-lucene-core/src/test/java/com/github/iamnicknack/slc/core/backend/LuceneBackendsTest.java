package com.github.iamnicknack.slc.core.backend;

import com.github.iamnicknack.slc.core.collection.LuceneCollection;
import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.github.iamnicknack.slc.core.index.MapDomainOperations;
import com.github.iamnicknack.slc.core.test.TestData;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LuceneBackendsTest {

    private FileSystem fs;

    @BeforeEach
    void beforeEach() throws IOException {
        fs = MemoryFileSystemBuilder.newLinux().build();
    }

    @AfterEach
    void afterEach() throws IOException {
        fs.close();
    }

    @Test
    void fileSystemIsUsable() throws IOException {
        Path root = fs.getPath("/virtual");
        Files.createDirectory(root);

        Path filePath = root.resolve("test.txt");
        Files.write(filePath, List.of("test"));

        var contents = Files.readAllLines(filePath);

        assertEquals(1, contents.size());
        assertEquals("test", contents.get(0));
    }

    @Test
    void canWriteSubDirectories() throws IOException {
        var path = fs.getPath("/virtual/one/two");
        Files.createDirectories(path);

        var filePath = path.resolve("test.txt");
        Files.write(filePath, List.of("test"));

        var contents = Files.readAllLines(filePath);

        assertEquals(1, contents.size());
        assertEquals("test", contents.get(0));
    }


    @Test
    void canWriteLuceneData() throws IOException {
        var path = fs.getPath("/test");
        var backend = LuceneBackends.directory(path);

        try(var stream = Files.list(path)) {
            assertEquals(2, stream.count());
        }

        var domain = new MapDomainOperations(TestData.documentDescriptor(backend));
        var collection = new LuceneCollection<>(domain, backend);
        collection.add(TestData.createValue("TEST"));
        assertEquals(1, collection.size());

        backend.close();
    }
}