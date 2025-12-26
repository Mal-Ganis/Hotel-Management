package com.hotelsystem.service;

import com.hotelsystem.entity.PriceStrategy;
import com.hotelsystem.entity.Room;
import com.hotelsystem.repository.PriceStrategyRepository;
import com.hotelsystem.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PriceStrategyService {

    private final PriceStrategyRepository priceStrategyRepository;
    private final RoomRepository roomRepository;

    public List<PriceStrategy> getAllPriceStrategies() {
        return priceStrategyRepository.findAll();
    }

    public List<PriceStrategy> getActivePriceStrategies() {
        return priceStrategyRepository.findByIsActiveTrue();
    }

    public Optional<PriceStrategy> getPriceStrategyById(Long id) {
        return priceStrategyRepository.findById(id);
    }

    public PriceStrategy createPriceStrategy(PriceStrategy strategy) {
        return priceStrategyRepository.save(strategy);
    }

    public PriceStrategy updatePriceStrategy(Long id, PriceStrategy strategy) {
        PriceStrategy existing = priceStrategyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("价格策略不存在"));

        existing.setName(strategy.getName());
        existing.setRoomType(strategy.getRoomType());
        existing.setType(strategy.getType());
        existing.setStartDate(strategy.getStartDate());
        existing.setEndDate(strategy.getEndDate());
        existing.setBasePrice(strategy.getBasePrice());
        existing.setDiscountRate(strategy.getDiscountRate());
        existing.setPriceAdjustment(strategy.getPriceAdjustment());
        existing.setDynamicFactor(strategy.getDynamicFactor());
        existing.setDescription(strategy.getDescription());
        existing.setIsActive(strategy.getIsActive());
        existing.setPriority(strategy.getPriority());

        return priceStrategyRepository.save(existing);
    }

    public void deletePriceStrategy(Long id) {
        if (!priceStrategyRepository.existsById(id)) {
            throw new RuntimeException("价格策略不存在");
        }
        priceStrategyRepository.deleteById(id);
    }

    /**
     * 计算房间在指定日期的实际价格（应用价格策略）
     */
    public BigDecimal calculateRoomPrice(Long roomId, LocalDate date) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("房间不存在"));

        BigDecimal basePrice = room.getPrice() != null ? room.getPrice() : BigDecimal.ZERO;

        // 获取适用于该日期和房型的有效策略（按优先级排序）
        List<PriceStrategy> applicableStrategies = priceStrategyRepository
                .findByStartDateLessThanEqualAndEndDateGreaterThanEqualAndIsActiveTrue(date, date)
                .stream()
                .filter(s -> s.getRoomType() == null || s.getRoomType().equals(room.getRoomType()))
                .sorted(Comparator.comparing(PriceStrategy::getPriority).reversed())
                .collect(Collectors.toList());

        BigDecimal finalPrice = basePrice;

        // 应用策略（优先级高的策略后应用）
        for (PriceStrategy strategy : applicableStrategies) {
            switch (strategy.getType()) {
                case FIXED:
                    if (strategy.getBasePrice() != null) {
                        finalPrice = strategy.getBasePrice();
                    }
                    break;
                case DISCOUNT:
                    if (strategy.getDiscountRate() != null) {
                        finalPrice = finalPrice.multiply(strategy.getDiscountRate());
                    }
                    break;
                case ADJUSTMENT:
                    if (strategy.getPriceAdjustment() != null) {
                        finalPrice = finalPrice.add(strategy.getPriceAdjustment());
                    }
                    break;
                case DYNAMIC:
                    // 动态价格：根据入住率等因素调整
                    if (strategy.getDynamicFactor() != null) {
                        // 简化处理：根据动态因子调整
                        // 实际应该根据实时入住率计算
                        finalPrice = finalPrice.multiply(strategy.getDynamicFactor());
                    }
                    break;
            }
        }

        // 确保价格不为负
        return finalPrice.max(BigDecimal.ZERO);
    }

    /**
     * 批量调整价格
     */
    public void batchUpdatePrices(String roomType, BigDecimal newPrice) {
        List<Room> rooms = roomRepository.findByRoomType(roomType);
        for (Room room : rooms) {
            room.setPrice(newPrice);
            roomRepository.save(room);
        }
    }
}

