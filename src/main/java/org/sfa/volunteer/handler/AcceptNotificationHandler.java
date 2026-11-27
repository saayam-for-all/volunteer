package org.sfa.volunteer.handler;

import org.sfa.volunteer.service.NotificationService;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;

public class AcceptNotificationHandler extends NotificationActionHandlerBase {
    public AcceptNotificationHandler() { super(true); }

    AcceptNotificationHandler(NotificationService notifications, UserService users,
                              ResponseBuilder responses, MessageSourceUtil messages) {
        super(true, notifications, users, responses, messages);
    }
}
