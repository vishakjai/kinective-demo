package com.meridianbranch.branchdesk;

import com.meridianbranch.branchdesk.scenario.ScenarioRunner;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Replays every scenario through the ScenarioRunner twice and asserts the trace
 * is byte-identical. The trace diff is only meaningful if the source oracle is
 * reproducible, so byte-stability is a hard requirement.
 */
class ScenarioByteStabilityTest {

    static Stream<Path> scenarioFiles() throws IOException {
        try (Stream<Path> s = Files.list(Paths.get("scenarios"))) {
            return s.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .collect(Collectors.toList())
                    .stream();
        }
    }

    @ParameterizedTest
    @MethodSource("scenarioFiles")
    void traceIsByteStable(Path scenarioFile) throws Exception {
        ScenarioRunner runner = new ScenarioRunner();

        String first = runner.run(runner.load(scenarioFile)).toJsonLines();
        String second = runner.run(runner.load(scenarioFile)).toJsonLines();

        assertEquals(first, second, "trace must be byte-stable across runs: " + scenarioFile);
        assertFalse(first.isBlank(), "scenario produced an empty trace: " + scenarioFile);

        // Every line is a complete JSON object record.
        List<String> lines = first.lines().collect(Collectors.toList());
        for (String line : lines) {
            assertEquals('{', line.charAt(0));
            assertEquals('}', line.charAt(line.length() - 1));
        }
    }
}
