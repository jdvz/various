package org.example;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.example.CalculateCodeLines.Flow.EXPANDED_STRING_LITERAL;
import static org.example.CalculateCodeLines.Flow.NORMAL;
import static org.example.CalculateCodeLines.Flow.STRING_LITERAL;

public class CalculateCodeLines {
  private static final Logger log = Logger.getLogger(CalculateCodeLines.class.getName());
  private static final Pattern NEW_LANE = Pattern.compile("\\R");
  private static final Pattern ALL_BREAKS = Pattern.compile("((?<![\\\\|\"])\"(?!\"{2}))|\"{3}|/\\*|\\*/|//");

  public static void main(String... args) {
    Path path = Path.of(args[0]);
    if (Files.exists(path) && Files.isRegularFile(path)) {
      try {
        String lanes = Files.readString(path);
        int count = new CalculateCodeLines().calculate(lanes);
        System.exit(count);
        return;
      } catch (IOException e) {
        log.warning("Parse error: " + e.getMessage());
      }
    }
    System.exit(-1);
  }

  protected int calculate(String lanes) {
    if (lanes == null || lanes.isBlank()) {
      return 0;
    }
    final AtomicReference<Flow> currentStatus = new AtomicReference<>(NORMAL);

    return (int) NEW_LANE.splitAsStream(lanes)
        .map(lane -> {
          LineSummary summary = new LineSummary();
          currentStatus.set(summary.process(lane, currentStatus.get()));
          return summary;
        })
        .filter(LineSummary::containsCode)
        .peek(lineSummary -> log.log(Level.INFO, lineSummary::toString))
        .count();
  }

  protected static class LineSummary {
    private static final Logger log = Logger.getLogger(LineSummary.class.getName());
    private final StringBuilder builder = new StringBuilder();
    private boolean significant;

    public boolean containsCode() {
      return significant; // to speedup
    }

    public Flow process(@NotNull String line, Flow status) {
      if (line.isBlank()) {
        return status;
      }
      return processFlow(line.trim(), status);
    }

    private Flow processFlow(String lane, Flow flow) {
      return switch (flow) {
        case NORMAL -> processNormalFlow(lane);
        case EXPANDED_STRING_LITERAL, STRING_LITERAL, MULTILINE_COMMENT -> processNestedFlow(lane, flow);
        case LINE_COMMENT -> NORMAL; // end of code
        default -> flowError(lane, flow);
      };
    }

    private static Flow flowError(String lane, Flow flow) {
      log.log(Level.SEVERE, () -> String.format("Unreachable lane '%s' for flow: %s", lane, flow));
      return flow;
    }

    /**
     * process normal flow
     *
     * @param lane string of code
     * @return new flow
     */
    private Flow processNormalFlow(String lane) {
      Matcher matcher = ALL_BREAKS.matcher(lane);
      if (matcher.find()) {
        String firstMatch = matcher.group();
        Flow newFlow = Flow.findByExample(firstMatch);
        int index = matcher.start();
        String valuablePartString = lane.substring(0, index).trim();
        markSignificantAndAddPart(valuablePartString);
        if (newFlow.terminal) { // no pair
          return NORMAL;
        } else {
          printFlow(newFlow);
          return processFlow(lane.substring(index + newFlow.minimalLength), newFlow);
        }
      } else {
        markSignificantAndAddPart(lane.trim());
        return NORMAL;
      }
    }

    /**
     * process complex flow of code
     *
     * @param lane string
     * @param flow flow
     * @return new flow if changed
     */
    private Flow processNestedFlow(String lane, Flow flow) {
      Matcher endMatcher = flow.end.matcher(lane);
      if (endMatcher.find()) {
        int endMatchingIndex = endMatcher.start();
        String afterBreaksLine = lane.substring(endMatchingIndex + flow.minimalLength);
        addStringLiteralAndSign(lane.substring(0, endMatchingIndex), flow);
        printFlow(flow);
        if (!afterBreaksLine.trim().isEmpty()) {
          this.significant = true;
          Matcher startMatcher = ALL_BREAKS.matcher(afterBreaksLine);
          if (startMatcher.find()) {
            String firstMatch = startMatcher.group();
            Flow newFlow = Flow.findByExample(firstMatch);
            printFlow(newFlow);
            int newIndex = afterBreaksLine.indexOf(firstMatch);
            this.builder.append(afterBreaksLine, 0, newIndex);
            return processFlow(afterBreaksLine.substring(newIndex + newFlow.minimalLength), newFlow);
          } else {
            this.builder.append(afterBreaksLine.trim());
            return NORMAL;
          }
        } else {
          printFlow(flow);
          return NORMAL;
        }
      } else {
        addStringLiteralAndSign(lane, flow);
      }
      return flow;
    }

    private void printFlow(Flow flow) {
      if (flow.printable) {
        this.builder.append(flow.pattern);
      }
    }

    private void markSignificantAndAddPart(String valuablePartString) {
      this.significant |= !valuablePartString.trim().isBlank();
      if (this.significant) {
        this.builder.append(valuablePartString.trim());
      }
    }

    private void addStringLiteralAndSign(String lane, Flow flow) {
      if (STRING_LITERAL == flow || EXPANDED_STRING_LITERAL == flow) {
        this.significant = true;
        this.builder.append(lane.trim()); // string literal matters!
      }
    }

    @Override
    public String toString() {
      return (this.significant ? "V:'" : "O:'") + builder + '\'';
    }
  }

  /**
   * code flow
   */
  protected enum Flow {
    // normal flow
    NORMAL("", null, 0, false, true),
    // string literal (comments and escaped quotes are ignored here)
    STRING_LITERAL("\"", Pattern.compile("(?<![\\\\|\"])\"(?!\"{2})"),
        Pattern.compile("(?<!\\\\)\"(?!\"{2})"), 1, false, true),
    // string multiline literal
    EXPANDED_STRING_LITERAL("\"\"\"", Pattern.compile("(?<!\\\\)\"\"\""), 3, false, true),
    // java comment, not a code
    MULTILINE_COMMENT("/*", Pattern.compile("(?<!\\\\)/\\*"), Pattern.compile("(?<!\\\\)\\*/"), 2, false, false),
    // java end of line comment, not a code
    LINE_COMMENT("//", Pattern.compile("//"), 2, true, false);

    private final String pattern;
    private final Pattern start; // probably to merge pattern from enums
    private final Pattern end;

    private final int minimalLength; // to speedup. pattern.lenght()
    private final boolean terminal;

    private final boolean printable;

    Flow(String pattern, Pattern start, Pattern end, int length, boolean terminal, boolean printable) {
      this.pattern = pattern;
      this.start = start;
      this.end = end;
      this.minimalLength = length;
      this.terminal = terminal;
      this.printable = printable;
    }

    Flow(String patternText, Pattern pattern, int length, boolean terminal, boolean printable) {
      this(patternText, pattern, pattern, length, terminal, printable);
    }

    public static Flow findByExample(String example) {
      return Arrays.stream(Flow.values())
          .filter(it -> it.pattern.equals(example))
          .findFirst()
          .orElseThrow();
    }
  }
}
