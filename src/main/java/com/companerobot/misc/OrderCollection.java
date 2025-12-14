package com.companerobot.misc;

import com.companerobot.enums.CountryCode;
import com.companerobot.enums.OrderStatus;
import com.companerobot.enums.OrderType;
import com.companerobot.helpers.CipherHelper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.util.Date;
import java.util.UUID;

import static com.companerobot.enums.OrderStatus.*;
import static com.companerobot.enums.OrderType.IMMEDIATE;
// تم إضافة هذا الاستيراد لدالة findActiveChatPartner
import static com.mongodb.client.model.Filters.or;

public class OrderCollection extends MongoBaseClass {

    protected static final MongoCollection<Document> orderCollection = database.getCollection("orders");

    public static void createNewOrder(Long userId) {
        Document document = new Document();
        document.put("orderId", String.valueOf(UUID.randomUUID()));
        document.put("userId", userId);
        document.put("pickUpAddress", null);
        document.put("pickUpAddressLongitude", null);
        document.put("pickUpAddressLatitude", null);
        document.put("destinationAddress", null);
        document.put("destinationAddressLongitude", null);
        document.put("destinationAddressLatitude", null);
        document.put("approximateDestinationAddress", null);
        document.put("countryCode", null);
        document.put("tripLengthInKilometers", null);
        document.put("notes", null);
        document.put("orderType", IMMEDIATE);
        document.put("orderStatus", WAITING_PICKUP_ADDRESS);
        document.put("postponedDepartureTime", null);
        document.put("price", null);
        document.put("currency", null);
        document.put("driverId", null);
        document.put("createdAt", new Date());
        orderCollection.insertOne(document);
    }
    
    // 🌟 الدالة الجديدة للدردشة الخاصة 🌟
    /**
     * تبحث عن الطرف الآخر في الطلب النشط الذي يسمح بالدردشة.
     * @param userId مُعرّف المرسل (الراكب أو السائق).
     * @return مُعرّف الطرف الآخر (السائق أو الراكب) أو null.
     */
    public static Long findActiveChatPartner(Long userId) {
        
        Document orderDoc = orderCollection.find(
            Filters.and(
                // البحث عن الطلب الذي يكون فيه المستخدم إما راكب أو سائق
                Filters.or(Filters.eq("userId", userId), Filters.eq("driverId", userId)),

                // الطلب يجب أن يكون في حالة تسمح بالدردشة (غير منتهٍ وغير ملغى وغير مُنتظر موافقة/سائق)
                Filters.ne("orderStatus", ORDER_FINISHED.toString()),
                Filters.ne("orderStatus", ORDER_CANCELED.toString()),
                Filters.ne("orderStatus", WAITING_DRIVER_APPROVAL.toString()),
                Filters.ne("orderStatus", WAITING_DRIVER.toString()),
                Filters.ne("orderStatus", WAITING_PICKUP_ADDRESS.toString())
            )
        ).first();

        if (orderDoc == null) {
            return null;
        }

        // تحديد الشريك وإرجاعه
        Long passengerId = orderDoc.getLong("userId"); 
        Long driverId = orderDoc.getLong("driverId");
        
        // يجب أن يكون driverId موجوداً أيضاً، وإلا لن تكون الدردشة مفيدة
        if (driverId == null) {
             return null;
        }

        if (userId.equals(passengerId)) {
            // المرسل هو الراكب، المستقبل هو السائق
            return driverId;
        } else if (userId.equals(driverId)) {
            // المرسل هو السائق، المستقبل هو الراكب
            return passengerId;
        }

        return null;
    }
    // نهاية الدالة findActiveChatPartner

    public static Document getOrderByPassengerIdAndStatus(Long userId, OrderStatus orderStatus) {
        return orderCollection.find(
                Filters.and(
                        Filters.eq("userId", userId),
                        Filters.eq("orderStatus", orderStatus))).first();
    }

    public static Document getUnfinishedOrderByPassengerId(Long userId) {
        return orderCollection.find(
                Filters.and(
                        Filters.eq("userId", userId),
                        Filters.ne("orderStatus", ORDER_FINISHED),
                        Filters.ne("orderStatus", ORDER_CANCELED))).first();
    }

    public static boolean isPassengerHasUnfinishedTrips(Long userId) {
        return orderCollection.find(
                Filters.and(
                        Filters.eq("userId", userId),
                        Filters.ne("orderStatus", ORDER_FINISHED),
                        Filters.ne("orderStatus", ORDER_CANCELED))).first() != null;

    }

    public static Document getUnfinishedOrderByDriverId(Long driverId) {
        return orderCollection.find(
                Filters.and(
                        Filters.eq("driverId", driverId),
                        Filters.ne("orderStatus", ORDER_FINISHED),
                        Filters.ne("orderStatus", ORDER_CANCELED))).first();
    }

    public static boolean isDriverHasUnfinishedTrips(Long driverId) {
        return orderCollection.find(
                Filters.and(
                        Filters.eq("driverId", driverId),
                        Filters.ne("orderStatus", ORDER_FINISHED),
                        Filters.ne("orderStatus", ORDER_CANCELED))).first() != null;

    }

    public static Document getOrderByDriverIdAndStatus(Long driverId, OrderStatus orderStatus) {
        return orderCollection.find(
                Filters.and(
                        Filters.eq("driverId", driverId),
                        Filters.eq("orderStatus", orderStatus))).first();
    }

    public static Document getOrderByOrderId(String orderId) {
        return orderCollection.find(Filters.eq("orderId", orderId)).first();
    }

    public static void setPickUpAddress(String orderId, String pickUpAddress) {
        orderCollection.updateOne(Filters.eq("orderId", orderId),
                Updates.set("pickUpAddress", CipherHelper.encrypt(pickUpAddress)));
    }

    public static void setCountryCode(String orderId, CountryCode countryCode) {
        orderCollection.updateOne(Filters.eq("orderId", orderId),
                Updates.set("countryCode", countryCode));
    }

    public static CountryCode getOrderCountryCode(String orderId) {
        return CountryCode.valueOf(orderCollection.find(Filters.eq("orderId", orderId))
                .first()
                .get("countryCode")
                .toString());
    }

    public static void updateOrderType(String orderId, OrderType orderType) {
        orderCollection.updateOne(Filters.eq("orderId", orderId),
                Updates.set("orderType", orderType));
    }

    public static OrderType getOrderType(String orderId) {
        return OrderType.valueOf(orderCollection.find(Filters.eq("orderId", orderId))
                .first()
                .get("orderType")
                .toString());
    }

    public static void setPostponedDepartureTime(String orderId, String postponedDepartureTime) {
        orderCollection.updateOne(Filters.eq("orderId", orderId),
                Updates.set("postponedDepartureTime", postponedDepartureTime));
    }

    public static void setPickUpAddressLongitude(String orderId, double longitude) {
        orderCollection.updateOne(Filters.eq("orderId", orderId),
                Updates.set("pickUpAddressLongitude", CipherHelper.encrypt(String.valueOf(longitude))));
    }

    public static void setPickUpAddressLatitude(String orderId, double latitude) {
        orderCollection.updateOne(Filters.eq("orderId", orderId),
                Updates.set("pickUpAddressLatitude", CipherHelper.encrypt(String.valueOf(latitude))));
    }


    public static void setDestinationAddress(String orderId, String destinationAddress) {
        orderCollection.updateOne(Filters.eq("orderId", orderId),
                Updates.set("destinationAddress", CipherHelper.encrypt(destinationAddress)));
    }

    public static void setDestinationAddressLongitude(String orderId, double longitude) {
        orderCollection.updateOne(Filters.eq("orderId", orderId),
                Updates.set("destinationAddressLongitude", CipherHelper.encrypt(String.valueOf(longitude))));
    }

    public static void setDestinationAddressLatitude(String orderId, double latitude) {
        orderCollection.updateOne(Filters.eq("orderId", orderId),
                Updates.set("destinationAddressLatitude", CipherHelper.encrypt(String.valueOf(latitude))));
    }

    public static void setApproximateDestinationAddress(String orderId, String approximateDestinationAddress) {
        orderCollection.updateOne(Filters.eq("orderId", orderId),
                Updates.set("approximateDestinationAddress", CipherHelper.encrypt(approximateDestinationAddress)));
    }

    public static void setTripLength(String orderId, double tripLength) {
        orderCollection.updateOne(Filters.eq("orderId", orderId),
                Updates.set("tripLengthInKilometers", tripLength));
    }

    public static double getTripLength(String orderId) {
        return orderCollection.find(Filters.eq("orderId", orderId))
                .first()
                .getDouble("tripLengthInKilometers");
    }

    public static void setNotes(String orderId, String notes) {
        orderCollection.updateOne(Filters.eq("orderId", orderId),
                Updates.set("notes", CipherHelper.encrypt(notes)));
    }

    public static void setDriverId(String orderId, Long driverId) {
        orderCollection.updateOne(Filters.eq("orderId", orderId),
                Updates.set("driverId", driverId));
    }

    public static void updateOrderStatus(String orderId, OrderStatus orderStatus) {
        orderCollection.updateOne(Filters.eq("orderId", orderId),
                Updates.set("orderStatus", orderStatus));
    }

    public static void setPriceForOrder(String orderId, double farePrice) {
        orderCollection.updateOne(Filters.eq("orderId", orderId),
                Updates.set("price", farePrice));
    }

    public static void setCurrencyForOrder(String orderId, String currency) {
        orderCollection.updateOne(Filters.eq("orderId", orderId),
                Updates.set("currency", currency));
    }
}
