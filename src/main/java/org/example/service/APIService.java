//package org.example.service;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import io.github.cdimascio.dotenv.Dotenv;
//import org.example.model.APIParam;
//import org.example.model.ModelResponse;
//
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.util.HashMap;
//import java.util.Map;
//
//public class APIService {
//    private static final APIService instance = new APIService();
//    private final HttpClient httpClient = HttpClient.newHttpClient();
//    private final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
//    private final String groqToken;
//    private final String togetherToken;
//    private final String groqGuide;
//    private final String togetherGuide;
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    public static APIService getInstance() {
//        return instance;
//    }
//
//    private APIService() {
//        groqToken = dotenv.get("GROQ_KEY");
//        togetherToken = dotenv.get("TOGETHER_KEY");
//        groqGuide = dotenv.get("GROQ_GUIDE");
//        togetherGuide = dotenv.get("TOGETHER_GUIDE");
//    }
//
//    public String callAPI(APIParam apiParam) throws Exception {
//        try {
//            // 디버깅용 로그
//            System.out.println("원본 프롬프트: " + apiParam.prompt());
//            System.out.println("모델: " + apiParam.model().platform + " - " + apiParam.model().name);
//
//            // 선택된 토글 값 추출
//            Map<String, String> toggleValues = extractToggleValues(apiParam.prompt());
//
//            // 향상된 프롬프트 생성
//            String enhancedPrompt = buildEnhancedPrompt(apiParam.prompt(), toggleValues);
//            System.out.println("향상된 프롬프트: " + enhancedPrompt);
//
//            String url;
//            String token;
//            String instruction;
//
//            switch (apiParam.model().platform) {
//                case GROQ -> {
//                    url = "https://api.groq.com/openai/v1/chat/completions";
//                    token = groqToken;
//                    instruction = groqGuide;
//                }
//                case TOGETHER -> {
//                    url = "https://api.together.xyz/v1/chat/completions";
//                    token = togetherToken;
//                    instruction = togetherGuide;
//                }
//                default -> throw new Exception("Unsupported platform: " + apiParam.model().platform);
//            }
//
//            // 안전한 JSON 문자열 생성을 위해 이스케이프 처리
//            String escapedInstruction = escapeJsonString(instruction);
//            String escapedPrompt = escapeJsonString(enhancedPrompt);
//
//            String body = """
//                    {
//                        "messages": [
//                            {
//                                "role": "system",
//                                "content": "%s"
//                            },
//                            {
//                                "role": "user",
//                                "content": "%s"
//                            }
//                        ],
//                        "model": "%s",
//                        "temperature": 0.7
//                    }
//                    """.formatted(escapedInstruction, escapedPrompt, apiParam.model().name);
//
//            // 요청 본문 로깅 (민감한 정보는 제외)
//            System.out.println("API 요청 본문(일부): " + body.substring(0, Math.min(100, body.length())) + "...");
//
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create(url))
//                    .POST(HttpRequest.BodyPublishers.ofString(body))
//                    .header("Authorization", "Bearer " + token)
//                    .header("Content-Type", "application/json")
//                    .build();
//
//            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
//
//            // 상태 코드 로깅
//            System.out.println("API 응답 상태 코드: " + response.statusCode());
//
//            if (response.statusCode() >= 400) {
//                System.err.println("API 오류 응답: " + response.body());
//                throw new Exception("API 호출 실패: " + response.statusCode() + " - " + response.body());
//            }
//
//            String responseBody = response.body();
//
//            // 응답 본문 로깅 (민감한 정보는 제외)
//            System.out.println("API 응답 본문(일부): " + responseBody.substring(0, Math.min(100, responseBody.length())) + "...");
//
//            // JSON 응답 파싱 시 예외 처리
//            ModelResponse modelResponse = objectMapper.readValue(responseBody, ModelResponse.class);
//
//            // 안전하게 content 추출
//            String content = "";
//            if (modelResponse.choices() != null && !modelResponse.choices().isEmpty() &&
//                    modelResponse.choices().get(0) != null &&
//                    modelResponse.choices().get(0).message() != null) {
//                content = modelResponse.choices().get(0).message().content();
//            } else {
//                System.err.println("API 응답 형식 오류: " + responseBody);
//                throw new Exception("API 응답 형식이 예상과 다릅니다");
//            }
//
//            Map<String, String> resultMap = new HashMap<>();
//            resultMap.put("content", content);
//            return objectMapper.writeValueAsString(resultMap);
//        } catch (Exception e) {
//            System.err.println("API 호출 중 예외 발생: " + e.getMessage());
//            e.printStackTrace();
//
//            Map<String, String> errorMap = new HashMap<>();
//            errorMap.put("error", "API 호출 중 오류: " + e.getMessage());
//            errorMap.put("content", "죄송합니다. API 호출 중 오류가 발생했습니다. 다시 시도해주세요.");
//
//            return objectMapper.writeValueAsString(errorMap);
//        }
//    }
//
//    /**
//     * JSON 문자열에 사용될 문자열을 이스케이프 처리하는 메서드
//     */
//    private String escapeJsonString(String input) {
//        if (input == null) {
//            return "";
//        }
//        return input.replace("\\", "\\\\")
//                .replace("\"", "\\\"")
//                .replace("\n", "\\n")
//                .replace("\r", "\\r")
//                .replace("\t", "\\t");
//    }
//
//    /**
//     * 프롬프트에서 토글 값을 추출하는 메서드
//     */
//    private Map<String, String> extractToggleValues(String prompt) {
//        Map<String, String> toggleValues = new HashMap<>();
//
//        // 기본값 설정
//        toggleValues.put("genre", "판타지");
//        toggleValues.put("length", "중간");
//        toggleValues.put("style", "현대적");
//        toggleValues.put("basePrompt", prompt != null ? prompt : "");
//
//        try {
//            if (prompt == null) {
//                return toggleValues;
//            }
//
//            // 소설 작성 요청 형식 처리
//            if (prompt.contains("소설 작성 요청:")) {
//                String[] lines = prompt.split("\n");
//                for (String line : lines) {
//                    if (line == null) continue;
//
//                    line = line.trim();
//                    if (line.startsWith("장르:")) {
//                        toggleValues.put("genre", line.substring("장르:".length()).trim());
//                    } else if (line.startsWith("길이:")) {
//                        toggleValues.put("length", line.substring("길이:".length()).trim());
//                    } else if (line.startsWith("문체:")) {
//                        toggleValues.put("style", line.substring("문체:".length()).trim());
//                    } else if (line.startsWith("설정:")) {
//                        toggleValues.put("basePrompt", line.substring("설정:".length()).trim());
//                    }
//                }
//            }
//            // 이전 챕터 내용 처리
//            else if (prompt.contains("이전 챕터 내용:")) {
//                String[] lines = prompt.split("\n");
//                for (String line : lines) {
//                    if (line == null) continue;
//
//                    line = line.trim();
//                    if (line.startsWith("이전 챕터 내용:")) {
//                        toggleValues.put("previousChapter", line.substring("이전 챕터 내용:".length()).trim());
//                    } else if (line.contains("다음 챕터(") && line.contains("장)")) {
//                        try {
//                            // 다음 챕터 번호 추출 (예: 다음 챕터(2장))
//                            int startIndex = line.indexOf("다음 챕터(") + "다음 챕터(".length();
//                            int endIndex = line.indexOf("장)");
//                            if (startIndex > 0 && endIndex > startIndex) {
//                                String chapterStr = line.substring(startIndex, endIndex).trim();
//                                toggleValues.put("nextChapter", chapterStr);
//                            }
//                        } catch (IndexOutOfBoundsException e) {
//                            System.err.println("챕터 번호 추출 중 오류: " + e.getMessage());
//                        }
//                    } else if (line.startsWith("장르는 ")) {
//                        String genreStyle = line.substring("장르는 ".length());
//                        if (genreStyle.contains(", 문체는 ")) {
//                            String[] parts = genreStyle.split(", 문체는 ");
//                            toggleValues.put("genre", parts[0].trim());
//                            if (parts.length > 1) {
//                                String stylePart = parts[1].trim();
//                                if (stylePart.endsWith("입니다.")) {
//                                    stylePart = stylePart.substring(0, stylePart.length() - "입니다.".length()).trim();
//                                }
//                                toggleValues.put("style", stylePart);
//                            }
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            System.err.println("토글 값 추출 중 오류: " + e.getMessage());
//            e.printStackTrace();
//            // 추출 실패 시 기본값 유지
//        }
//
//        return toggleValues;
//    }
//
//    /**
//     * 토글 값을 기반으로 향상된 프롬프트를 구성하는 메서드
//     */
//    private String buildEnhancedPrompt(String originalPrompt, Map<String, String> toggleValues) {
//        try {
//            // 이어서 생성 (다음 챕터) 케이스
//            if (toggleValues.containsKey("previousChapter")) {
//                String nextChapter = toggleValues.getOrDefault("nextChapter", "다음");
//
//                return String.format("""
//                    당신은 뛰어난 소설가입니다. 다음 정보를 바탕으로 소설의 다음 챕터를 작성해주세요.
//
//                    장르: %s
//                    문체: %s
//
//                    이전 내용 요약:
//                    %s
//
//                    %s장을 작성해주세요. 일관된 등장인물과 세계관을 유지하며, 이전 내용과 자연스럽게 이어지도록 해주세요.
//                    주의: 다른 언어로 응답할 경우 시스템 오류가 발생합니다. 반드시 한국어로만 응답하세요.
//                    """,
//                        toggleValues.getOrDefault("genre", "지정되지 않음"),
//                        toggleValues.getOrDefault("style", "지정되지 않음"),
//                        toggleValues.get("previousChapter"),
//                        nextChapter
//                );
//            }
//
//            // 첫 챕터 생성 케이스 - 소설 작성 요청 형식이 있는 경우
//            if (originalPrompt != null && originalPrompt.contains("소설 작성 요청:")) {
//                return originalPrompt;
//            }
//
//            // 기본 케이스 - 일반 프롬프트
//            String lengthGuidance;
//            switch (toggleValues.getOrDefault("length", "중간")) {
//                case "짧은" -> lengthGuidance = "약 1-2페이지 분량 (1000-2000자)";
//                case "긴" -> lengthGuidance = "약 7-10페이지 분량 (7000-10000자)";
//                default -> lengthGuidance = "약 3-5페이지 분량 (3000-5000자)";
//            }
//
//            return String.format("""
//                당신은 뛰어난 소설가입니다. 다음 정보를 바탕으로 소설을 작성해주세요.
//
//                장르: %s
//                문체: %s
//                길이: %s
//
//                다음 설정에 맞는 매력적인 소설을 작성해주세요:
//                %s
//
//                소설에는 적절한 제목을 포함해주세요. 창의적이고 몰입감 있는 스토리를 만들어주세요.
//                주의: 다른 언어로 응답할 경우 시스템 오류가 발생합니다. 반드시 한국어로만 응답하세요.
//                """,
//                    toggleValues.get("genre"),
//                    toggleValues.get("style"),
//                    lengthGuidance,
//                    toggleValues.get("basePrompt")
//            );
//        } catch (Exception e) {
//            System.err.println("프롬프트 구성 중 오류: " + e.getMessage());
//            e.printStackTrace();
//            // 오류 발생 시 기본 프롬프트 반환
//            return originalPrompt != null ? originalPrompt : "소설을 작성해주세요.";
//        }
//    }
//}

package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import org.example.model.APIParam;
import org.example.model.ModelResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class APIService {
    private static final APIService instance = new APIService();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    private final String geminiKey;
    private final String geminiGuide;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static APIService getInstance() {
        return instance;
    }

    private APIService() {
        geminiKey = dotenv.get("GEMINI_KEY");
        geminiGuide = dotenv.get("GEMINI_GUIDE");
    }

    public String callAPI(APIParam apiParam) throws Exception {
        try {
            // 디버깅용 로그
            System.out.println("원본 프롬프트: " + apiParam.prompt());
            System.out.println("모델: Gemini - " + apiParam.model().name);

            // 선택된 토글 값 추출
            Map<String, String> toggleValues = extractToggleValues(apiParam.prompt());

            // 향상된 프롬프트 생성
            String enhancedPrompt = buildEnhancedPrompt(apiParam.prompt(), toggleValues);
            System.out.println("향상된 프롬프트: " + enhancedPrompt);

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

            // Gemini API 요청 본문 구성
            Map<String, Object> bodyMap = new HashMap<>();

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", enhancedPrompt);

            // 시스템 명령어가 있는 경우 추가
            if (geminiGuide != null && !geminiGuide.isEmpty()) {
                Map<String, Object> systemPart = new HashMap<>();
                systemPart.put("text", geminiGuide);

                Map<String, Object> systemMessage = new HashMap<>();
                systemMessage.put("role", "system");
                systemMessage.put("parts", List.of(systemPart));

                Map<String, Object> userMessage = new HashMap<>();
                userMessage.put("role", "user");
                userMessage.put("parts", List.of(textPart));

                bodyMap.put("contents", List.of(systemMessage, userMessage));
            } else {
                // 시스템 명령어가 없는 경우 사용자 메시지만 전송
                Map<String, Object> content = new HashMap<>();
                content.put("role", "user");
                content.put("parts", List.of(textPart));

                bodyMap.put("contents", List.of(content));
            }

            // 온도 설정 (0.7은 기본값)
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            bodyMap.put("generationConfig", generationConfig);

            String bodyString = objectMapper.writeValueAsString(bodyMap);

            // 요청 본문 로깅 (민감한 정보는 제외)
            System.out.println("API 요청 본문(일부): " + bodyString.substring(0, Math.min(100, bodyString.length())) + "...");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "?key=" + geminiKey))
                    .POST(HttpRequest.BodyPublishers.ofString(bodyString))
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // 상태 코드 로깅
            System.out.println("API 응답 상태 코드: " + response.statusCode());

            if (response.statusCode() >= 400) {
                System.err.println("API 오류 응답: " + response.body());
                throw new Exception("API 호출 실패: " + response.statusCode() + " - " + response.body());
            }

            String responseBody = response.body();

            // 응답 본문 로깅 (민감한 정보는 제외)
            System.out.println("API 응답 본문(일부): " + responseBody.substring(0, Math.min(100, responseBody.length())) + "...");

            // Gemini API 응답 처리
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

            String content = "";
            // Gemini 응답 구조에서 텍스트 추출
            if (responseMap.containsKey("candidates") && responseMap.get("candidates") instanceof List) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> candidate = candidates.get(0);
                    if (candidate.containsKey("content") && candidate.get("content") instanceof Map) {
                        Map<String, Object> contentMap = (Map<String, Object>) candidate.get("content");
                        if (contentMap.containsKey("parts") && contentMap.get("parts") instanceof List) {
                            List<Map<String, Object>> parts = (List<Map<String, Object>>) contentMap.get("parts");
                            if (!parts.isEmpty() && parts.get(0).containsKey("text")) {
                                content = (String) parts.get(0).get("text");
                            }
                        }
                    }
                }
            }

            if (content.isEmpty()) {
                System.err.println("API 응답 형식 오류: " + responseBody);
                throw new Exception("API 응답 형식이 예상과 다릅니다");
            }

            Map<String, String> resultMap = new HashMap<>();
            resultMap.put("content", content);
            return objectMapper.writeValueAsString(resultMap);
        } catch (Exception e) {
            System.err.println("API 호출 중 예외 발생: " + e.getMessage());
            e.printStackTrace();

            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "API 호출 중 오류: " + e.getMessage());
            errorMap.put("content", "죄송합니다. API 호출 중 오류가 발생했습니다. 다시 시도해주세요.");

            return objectMapper.writeValueAsString(errorMap);
        }
    }

    /**
     * 프롬프트에서 토글 값을 추출하는 메서드
     */
    private Map<String, String> extractToggleValues(String prompt) {
        Map<String, String> toggleValues = new HashMap<>();

        // 기본값 설정
        toggleValues.put("genre", "판타지");
        toggleValues.put("length", "중간");
        toggleValues.put("style", "현대적");
        toggleValues.put("basePrompt", prompt != null ? prompt : "");

        try {
            if (prompt == null) {
                return toggleValues;
            }

            // 소설 작성 요청 형식 처리
            if (prompt.contains("소설 작성 요청:")) {
                String[] lines = prompt.split("\n");
                for (String line : lines) {
                    if (line == null) continue;

                    line = line.trim();
                    if (line.startsWith("장르:")) {
                        toggleValues.put("genre", line.substring("장르:".length()).trim());
                    } else if (line.startsWith("길이:")) {
                        toggleValues.put("length", line.substring("길이:".length()).trim());
                    } else if (line.startsWith("문체:")) {
                        toggleValues.put("style", line.substring("문체:".length()).trim());
                    } else if (line.startsWith("설정:")) {
                        toggleValues.put("basePrompt", line.substring("설정:".length()).trim());
                    }
                }
            }
            // 이전 챕터 내용 처리
            else if (prompt.contains("이전 챕터 내용:")) {
                String[] lines = prompt.split("\n");
                for (String line : lines) {
                    if (line == null) continue;

                    line = line.trim();
                    if (line.startsWith("이전 챕터 내용:")) {
                        toggleValues.put("previousChapter", line.substring("이전 챕터 내용:".length()).trim());
                    } else if (line.contains("다음 챕터(") && line.contains("장)")) {
                        try {
                            // 다음 챕터 번호 추출 (예: 다음 챕터(2장))
                            int startIndex = line.indexOf("다음 챕터(") + "다음 챕터(".length();
                            int endIndex = line.indexOf("장)");
                            if (startIndex > 0 && endIndex > startIndex) {
                                String chapterStr = line.substring(startIndex, endIndex).trim();
                                toggleValues.put("nextChapter", chapterStr);
                            }
                        } catch (IndexOutOfBoundsException e) {
                            System.err.println("챕터 번호 추출 중 오류: " + e.getMessage());
                        }
                    } else if (line.startsWith("장르는 ")) {
                        String genreStyle = line.substring("장르는 ".length());
                        if (genreStyle.contains(", 문체는 ")) {
                            String[] parts = genreStyle.split(", 문체는 ");
                            toggleValues.put("genre", parts[0].trim());
                            if (parts.length > 1) {
                                String stylePart = parts[1].trim();
                                if (stylePart.endsWith("입니다.")) {
                                    stylePart = stylePart.substring(0, stylePart.length() - "입니다.".length()).trim();
                                }
                                toggleValues.put("style", stylePart);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("토글 값 추출 중 오류: " + e.getMessage());
            e.printStackTrace();
            // 추출 실패 시 기본값 유지
        }

        return toggleValues;
    }

    /**
     * 토글 값을 기반으로 향상된 프롬프트를 구성하는 메서드
     */
    private String buildEnhancedPrompt(String originalPrompt, Map<String, String> toggleValues) {
        try {
            // 이어서 생성 (다음 챕터) 케이스
            if (toggleValues.containsKey("previousChapter")) {
                String nextChapter = toggleValues.getOrDefault("nextChapter", "다음");

                return String.format("""
                    당신은 뛰어난 소설가입니다. 다음 정보를 바탕으로 소설의 다음 챕터를 작성해주세요.

                    장르: %s
                    문체: %s

                    이전 내용 요약:
                    %s

                    %s장을 작성해주세요. 일관된 등장인물과 세계관을 유지하며, 이전 내용과 자연스럽게 이어지도록 해주세요.
                    주의: 다른 언어로 응답할 경우 시스템 오류가 발생합니다. 반드시 한국어로만 응답하세요.
                    """,
                        toggleValues.getOrDefault("genre", "지정되지 않음"),
                        toggleValues.getOrDefault("style", "지정되지 않음"),
                        toggleValues.get("previousChapter"),
                        nextChapter
                );
            }

            // 첫 챕터 생성 케이스 - 소설 작성 요청 형식이 있는 경우
            if (originalPrompt != null && originalPrompt.contains("소설 작성 요청:")) {
                return originalPrompt;
            }

            // 기본 케이스 - 일반 프롬프트
            String lengthGuidance;
            switch (toggleValues.getOrDefault("length", "중간")) {
                case "짧은" -> lengthGuidance = "약 1-2페이지 분량 (1000-2000자)";
                case "긴" -> lengthGuidance = "약 7-10페이지 분량 (7000-10000자)";
                default -> lengthGuidance = "약 3-5페이지 분량 (3000-5000자)";
            }

            return String.format("""
                당신은 뛰어난 소설가입니다. 다음 정보를 바탕으로 소설을 작성해주세요.

                장르: %s
                문체: %s
                길이: %s

                다음 설정에 맞는 매력적인 소설을 작성해주세요:
                %s

                소설에는 적절한 제목을 포함해주세요. 창의적이고 몰입감 있는 스토리를 만들어주세요.
                주의: 다른 언어로 응답할 경우 시스템 오류가 발생합니다. 반드시 한국어로만 응답하세요.
                """,
                    toggleValues.get("genre"),
                    toggleValues.get("style"),
                    lengthGuidance,
                    toggleValues.get("basePrompt")
            );
        } catch (Exception e) {
            System.err.println("프롬프트 구성 중 오류: " + e.getMessage());
            e.printStackTrace();
            // 오류 발생 시 기본 프롬프트 반환
            return originalPrompt != null ? originalPrompt : "소설을 작성해주세요.";
        }
    }
}