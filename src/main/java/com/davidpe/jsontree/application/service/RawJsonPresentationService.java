package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.application.model.RawJsonPresentation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RawJsonPresentationService {

  private final ObjectMapper objectMapper;
  private final BestEffortJsonPrettyPrinter bestEffortJsonPrettyPrinter;

  @Autowired
  public RawJsonPresentationService(
      ObjectMapper objectMapper, BestEffortJsonPrettyPrinter bestEffortJsonPrettyPrinter) {
    this.objectMapper = objectMapper;
    this.bestEffortJsonPrettyPrinter = bestEffortJsonPrettyPrinter;
  }

  public RawJsonPresentationService(ObjectMapper objectMapper) {
    this(objectMapper, new BestEffortJsonPrettyPrinter());
  }

  public RawJsonPresentation present(String rawJson) {
    if (rawJson == null || rawJson.isEmpty()) {
      return new RawJsonPresentation("", new int[] {0});
    }

    try {
      return prettyPrintedPresentation(rawJson);
    } catch (JsonProcessingException exception) {
      return new RawJsonPresentation(rawJson, identityBoundaries(rawJson.length()));
    }
  }

  public RawJsonPresentation presentLargePreviewChunk(
      String rawJson, boolean prettyOnLargePreviewEnabled) {
    if (rawJson == null || rawJson.isEmpty()) {
      return new RawJsonPresentation("", new int[] {0});
    }

    try {
      return prettyPrintedPresentation(rawJson);
    } catch (JsonProcessingException exception) {
      if (!prettyOnLargePreviewEnabled) {
        return new RawJsonPresentation(rawJson, identityBoundaries(rawJson.length()));
      }
      String formattedChunk = bestEffortJsonPrettyPrinter.prettyPrint(rawJson);
      return new RawJsonPresentation(formattedChunk, buildBoundaryMap(rawJson, formattedChunk));
    }
  }

  private RawJsonPresentation prettyPrintedPresentation(String rawJson)
      throws JsonProcessingException {
    String prettyJson =
        objectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(objectMapper.readTree(rawJson));
    return new RawJsonPresentation(prettyJson, buildBoundaryMap(rawJson, prettyJson));
  }

  private int[] buildBoundaryMap(String source, String display) {
    SignificantCharacterScan sourceScan = scanSignificantCharacters(source);
    DisplayBoundaryScan displayScan =
        scanDisplayBoundaries(display, sourceScan.ordinalsByBoundary()[source.length()]);
    int[] boundaries = new int[source.length() + 1];

    for (int index = 0; index < boundaries.length; index++) {
      int consumedSignificant = sourceScan.ordinalsByBoundary()[index];
      if (index < source.length() && sourceScan.significantByCharacter()[index]) {
        boundaries[index] = displayScan.startBoundaryByOrdinal()[consumedSignificant + 1];
        continue;
      }
      boundaries[index] = displayScan.endBoundaryByOrdinal()[consumedSignificant];
    }
    return boundaries;
  }

  private SignificantCharacterScan scanSignificantCharacters(String text) {
    int[] ordinals = new int[text.length() + 1];
    boolean[] significant = new boolean[text.length()];
    JsonTextState state = new JsonTextState();
    int count = 0;
    ordinals[0] = 0;

    for (int index = 0; index < text.length(); index++) {
      char current = text.charAt(index);
      boolean currentSignificant = !state.isStructuralWhitespace(current);
      significant[index] = currentSignificant;
      if (currentSignificant) {
        count++;
      }
      state.advance(current);
      ordinals[index + 1] = count;
    }
    return new SignificantCharacterScan(ordinals, significant);
  }

  private DisplayBoundaryScan scanDisplayBoundaries(String text, int significantCount) {
    int[] startBoundaries = new int[significantCount + 1];
    int[] endBoundaries = new int[significantCount + 1];
    JsonTextState state = new JsonTextState();
    int count = 0;
    startBoundaries[0] = 0;
    endBoundaries[0] = 0;

    for (int index = 0; index < text.length(); index++) {
      char current = text.charAt(index);
      if (!state.isStructuralWhitespace(current)) {
        count++;
        if (count < startBoundaries.length) {
          startBoundaries[count] = index;
          endBoundaries[count] = index + 1;
        }
      }
      state.advance(current);
    }

    for (int index = count + 1; index < startBoundaries.length; index++) {
      startBoundaries[index] = text.length();
      endBoundaries[index] = text.length();
    }
    return new DisplayBoundaryScan(startBoundaries, endBoundaries);
  }

  private int[] identityBoundaries(int length) {
    int[] boundaries = new int[length + 1];
    for (int index = 0; index <= length; index++) {
      boundaries[index] = index;
    }
    return boundaries;
  }

  private static final class JsonTextState {

    private boolean insideString;
    private boolean escaping;

    private boolean isStructuralWhitespace(char current) {
      return !insideString && Character.isWhitespace(current);
    }

    private void advance(char current) {
      if (escaping) {
        escaping = false;
        return;
      }
      if (insideString && current == '\\') {
        escaping = true;
        return;
      }
      if (current == '"') {
        insideString = !insideString;
      }
    }
  }

  private record SignificantCharacterScan(
      int[] ordinalsByBoundary, boolean[] significantByCharacter) {}

  private record DisplayBoundaryScan(int[] startBoundaryByOrdinal, int[] endBoundaryByOrdinal) {}
}
