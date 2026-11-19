package org.sfa.volunteer.handler;

import org.sfa.volunteer.service.NotificationService;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;

public class DenyNotificationHandler extends NotificationActionHandlerBase {
    public DenyNotificationHandler() { super(false); }

    DenyNotificationHandler(NotificationService notifications, UserService users,
                            ResponseBuilder responses, MessageSourceUtil messages) {
        super(false, notifications, users, responses, messages);
    }
}
