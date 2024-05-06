// package com.generac.ces.systemgateway.validator;
//
// import static org.junit.jupiter.api.Assertions.assertFalse;
// import static org.junit.jupiter.api.Assertions.assertTrue;
//
// import com.generac.ces.systemgateway.model.SystemModeControlMessageRequest;
// import com.generac.ces.systemgateway.model.common.SystemMode;
// import java.time.OffsetDateTime;
// import java.util.ArrayList;
// import java.util.Arrays;
// import org.junit.Test;
//
// public class ForbiddenSysModeControlValidatorTest {
//
//    @Test
//    public void testIsValid_valid_SystemInstantControlMessage() {
//        ForbiddenSysModeControlValidator validator = new ForbiddenSysModeControlValidator();
//
//        SystemModeControlMessageRequest request = new SystemModeControlMessageRequest();
//        SystemModeControlMessageRequest.ControlMessage controlMessage =
//                new SystemModeControlMessageRequest.ControlMessage();
//        controlMessage.setSystemMode(SystemMode.SELL);
//        request.setControlMessages(
//                Arrays.stream(new SystemModeControlMessageRequest.ControlMessage[]
// {controlMessage})
//                        .toList());
//
//        boolean isValid = validator.isValid(request, null);
//
//        assertTrue(isValid);
//    }
//
//    @Test
//    public void testIsValid_invalid_emptyControlList() {
//        ForbiddenSysModeControlValidator validator = new ForbiddenSysModeControlValidator();
//
//        SystemModeControlMessageRequest request = new SystemModeControlMessageRequest();
//        request.setControlMessages(new ArrayList<>());
//
//        boolean isValid = validator.isValid(request, null);
//
//        assertFalse(isValid);
//    }
//
//    @Test
//    public void testIsValid_invalid_nullControlList() {
//        ForbiddenSysModeControlValidator validator = new ForbiddenSysModeControlValidator();
//
//        SystemModeControlMessageRequest request = new SystemModeControlMessageRequest();
//
//        boolean isValid = validator.isValid(request, null);
//
//        assertFalse(isValid);
//    }
//
//    @Test
//    public void testIsValid_invalid_SystemInstantControlMessage_forbiddenMode() {
//        ForbiddenSysModeControlValidator validator = new ForbiddenSysModeControlValidator();
//
//        SystemModeControlMessageRequest request = new SystemModeControlMessageRequest();
//        SystemModeControlMessageRequest.ControlMessage controlMessage =
//                new SystemModeControlMessageRequest.ControlMessage();
//        controlMessage.setSystemMode(SystemMode.SAFETY_SHUTDOWN);
//        request.setControlMessages(
//                Arrays.stream(new SystemModeControlMessageRequest.ControlMessage[]
// {controlMessage})
//                        .toList());
//
//        boolean isValid = validator.isValid(request, null);
//
//        assertFalse(isValid);
//    }
//
//    @Test
//    public void testIsValid_valid_SingleSystemScheduleControlMessage() {
//        ForbiddenSysModeControlValidator validator = new ForbiddenSysModeControlValidator();
//
//        SystemModeControlMessageRequest request = new SystemModeControlMessageRequest();
//        SystemModeControlMessageRequest.ControlMessage controlMessage =
//                new SystemModeControlMessageRequest.ControlMessage();
//        controlMessage.setDuration(5L);
//        controlMessage.setStartTime(OffsetDateTime.now());
//        controlMessage.setSystemMode(SystemMode.SELL);
//        request.setControlMessages(
//                Arrays.stream(new SystemModeControlMessageRequest.ControlMessage[]
// {controlMessage})
//                        .toList());
//
//        boolean isValid = validator.isValid(request, null);
//
//        assertTrue(isValid);
//    }
//
//    @Test
//    public void testIsValid_valid_MultipleSystemScheduleControlMessage() {
//        ForbiddenSysModeControlValidator validator = new ForbiddenSysModeControlValidator();
//
//        SystemModeControlMessageRequest request = new SystemModeControlMessageRequest();
//
//        SystemModeControlMessageRequest.ControlMessage controlMessage =
//                new SystemModeControlMessageRequest.ControlMessage();
//        controlMessage.setDuration(5L);
//        controlMessage.setStartTime(OffsetDateTime.now());
//        controlMessage.setSystemMode(SystemMode.SELL);
//
//        SystemModeControlMessageRequest.ControlMessage secondControlMessage =
//                new SystemModeControlMessageRequest.ControlMessage();
//        secondControlMessage.setDuration(5L);
//        secondControlMessage.setStartTime(OffsetDateTime.now().plusDays(5));
//        secondControlMessage.setSystemMode(SystemMode.PRIORITY_BACKUP);
//
//        request.setControlMessages(
//                Arrays.stream(
//                                new SystemModeControlMessageRequest.ControlMessage[] {
//                                    controlMessage, secondControlMessage
//                                })
//                        .toList());
//
//        boolean isValid = validator.isValid(request, null);
//
//        assertTrue(isValid);
//    }
//
//    @Test
//    public void testIsValid_invalid_SingleSystemScheduleControlMessage_forbiddenMode() {
//        ForbiddenSysModeControlValidator validator = new ForbiddenSysModeControlValidator();
//
//        SystemModeControlMessageRequest request = new SystemModeControlMessageRequest();
//
//        SystemModeControlMessageRequest.ControlMessage controlMessage =
//                new SystemModeControlMessageRequest.ControlMessage();
//        controlMessage.setDuration(5L);
//        controlMessage.setStartTime(OffsetDateTime.now());
//        controlMessage.setSystemMode(SystemMode.SELL);
//
//        // Last message has the forbidden mode
//        SystemModeControlMessageRequest.ControlMessage secondControlMessage =
//                new SystemModeControlMessageRequest.ControlMessage();
//        secondControlMessage.setDuration(5L);
//        secondControlMessage.setStartTime(OffsetDateTime.now().plusDays(5));
//        secondControlMessage.setSystemMode(SystemMode.SAFETY_SHUTDOWN);
//
//        request.setControlMessages(
//                Arrays.stream(
//                                new SystemModeControlMessageRequest.ControlMessage[] {
//                                    controlMessage, secondControlMessage
//                                })
//                        .toList());
//
//        boolean isValid = validator.isValid(request, null);
//
//        assertFalse(isValid);
//    }
//
//    @Test
//    public void testIsValid_invalid_SingleSystemScheduleControlMessage_missingSchedule() {
//        ForbiddenSysModeControlValidator validator = new ForbiddenSysModeControlValidator();
//
//        SystemModeControlMessageRequest request = new SystemModeControlMessageRequest();
//
//        SystemModeControlMessageRequest.ControlMessage controlMessage =
//                new SystemModeControlMessageRequest.ControlMessage();
//        controlMessage.setDuration(5L);
//        controlMessage.setStartTime(OffsetDateTime.now());
//        controlMessage.setSystemMode(SystemMode.SELL);
//
//        // Last message is missing the schedule component
//        SystemModeControlMessageRequest.ControlMessage secondControlMessage =
//                new SystemModeControlMessageRequest.ControlMessage();
//        secondControlMessage.setSystemMode(SystemMode.PRIORITY_BACKUP);
//
//        request.setControlMessages(
//                Arrays.stream(
//                                new SystemModeControlMessageRequest.ControlMessage[] {
//                                    controlMessage, secondControlMessage
//                                })
//                        .toList());
//
//        boolean isValid = validator.isValid(request, null);
//
//        assertFalse(isValid);
//    }
// }
