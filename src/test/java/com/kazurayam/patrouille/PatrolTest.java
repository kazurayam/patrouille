package com.kazurayam.patrouille;

import com.kazurayam.materialstore.filesystem.Store;
import com.kazurayam.materialstore.filesystem.Stores;
import com.kazurayam.patrouille.missions.TsumitateNISA;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class PatrolTest {

    private static Store store;

    @BeforeAll
    public static void beforeAll() throws IOException {
        Path projectDir = Paths.get(System.getProperty("user.dir"));
        Path outputDir = projectDir.resolve("build/tmp/testOutput")
                .resolve(PatrolTest.class.getName());
        Files.createDirectories(outputDir);
        Path root = outputDir.resolve("store");
        store = Stores.newInstance(root);
    }

    @BeforeEach
    public void setup() {}

    @Test
    public void test_smoke() throws IOException {
        Patrol patrol = new Patrol(store);
        List<Mission> missions = Collections.singletonList(new TsumitateNISA());
        patrol.carryoutAll(missions);
    }
}
