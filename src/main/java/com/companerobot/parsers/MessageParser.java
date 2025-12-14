package com.companerobot.parsers;

import org.telegram.telegrambots.meta.api.objects.message.Message;
// الاستيرادات الضرورية للتعديل:
import com.companerobot.misc.OrderCollection;
import com.companerobot.helpers.MessageExecutionHelper; 
import com.companerobot.misc.UserCollection; 
import com.companerobot.enums.UserRole;

import static com.companerobot.enums.UserRole.DRIVER;
import static com.companerobot.enums.UserRole.PASSENGER;
import static com.companerobot.parsers.message_parsers.DriverMessageParser.parseDriverMessage;
import static com.companerobot.parsers.message_parsers.MiscMessageParser.parseMiscMessage;
import static com.companerobot.parsers.message_parsers.PassengerMessageParser.parsePassengerMessage;
// إذا كان لديك استيرادات أخرى غير موجودة هنا، يجب إضافتها.


public class MessageParser {

    public static void parseMessage(Message message) {
        Long userId = message.getFrom().getId();

        // 🌟 1. منطق الدردشة الخاصة (تمت إضافته حديثاً) 🌟
        // التحقق من أنها رسالة نصية وليست أمراً (لا تبدأ بـ '/')
        if (message.hasText() && !message.getText().startsWith("/")) {
             Long chatPartnerId = OrderCollection.findActiveChatPartner(userId);

            if (chatPartnerId != null) {
                // إرسال الرسالة إلى الشريك الآخر
                MessageExecutionHelper.forwardMessageWithRoleTag(
                    message.getChatId(), 
                    message.getMessageId(), 
                    chatPartnerId, 
                    userId 
                );
                return; // 🛑 التوقف هنا ومنع معالجة الرسالة كأمر عادي
            }
        }
        // -----------------------------------------------------------------

        // 2. منطق توجيه الرسائل القديم (تم نقله من الأعلى لضمان عمل الدردشة أولاً)
        
        // يجب أن نتحقق من وجود المستخدم قبل قراءة دوره لتجنب NullPointerException
        // (إذا كنت قد أضفت دالة التحقق isUserExist سابقاً، فهذا هو مكانها المناسب)
        
        UserRole userRole = UserCollection.getUserRole(userId);

        if (userRole == PASSENGER) {
            parsePassengerMessage(message);

        } else if (userRole == DRIVER) {
            parseDriverMessage(message);

//        } else if (userRole == SUPER_ADMIN) {
//            parseSuperAdminMessage(message);
        } else {
            parseMiscMessage(message);
        }
    }
}
