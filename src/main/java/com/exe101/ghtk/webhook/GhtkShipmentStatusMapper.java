package com.exe101.ghtk.webhook;

import com.exe101.shipping.entity.ShipmentStatus;

import java.util.Set;

public final class GhtkShipmentStatusMapper {

    private static final Set<Integer> SHIPPER_ONLY_STATUS_IDS = Set.of(123, 127, 128, 45, 49, 410);

    private GhtkShipmentStatusMapper() {
    }

    public static boolean isShipperOnlyStatus(Integer statusId) {
        return statusId != null && SHIPPER_ONLY_STATUS_IDS.contains(statusId);
    }

    public static ShipmentStatus toShipmentStatus(Integer statusId) {
        if (statusId == null) {
            return null;
        }

        return switch (statusId) {
            case 1 -> ShipmentStatus.PENDING_PICKUP;
            case 2 -> ShipmentStatus.ACCEPTED;
            case 12 -> ShipmentStatus.PENDING_PICKUP;
            case 3 -> ShipmentStatus.PICKED_UP;
            case 4 -> ShipmentStatus.DELIVERING;
            case 5 -> ShipmentStatus.DELIVERED;
            case 6 -> ShipmentStatus.RECONCILED;
            case -1 -> ShipmentStatus.CANCELED;
            case 7 -> ShipmentStatus.PICKUP_FAILED;
            case 8 -> ShipmentStatus.PICKUP_DELAYED;
            case 9 -> ShipmentStatus.DELIVERY_FAILED;
            case 10 -> ShipmentStatus.DELIVERY_DELAYED;
            case 20 -> ShipmentStatus.RETURNING;
            case 21 -> ShipmentStatus.RETURNED;
            default -> null;
        };
    }
}
