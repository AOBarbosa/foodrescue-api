package br.com.seudominio.foodrescue.domain;

import br.com.seudominio.perishrescue.domain.builders.ResourceBuilder;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import org.hibernate.envers.Audited;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * FoodItem: a food product or ready meal rescued before the offerer closes
 * (breads, cakes, lunch boxes, surprise bags).
 *
 * <p>FoodRescue's implementation of <b>hot spot 1</b>: adds the allergen
 * information and the storage temperature range to the kernel's
 * {@link PerishableResource}, and defines the end of a food item's useful life
 * as the offerer's closing time today, or the end of its expiration date if
 * that comes first.</p>
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Entity
@Audited
@Table(name = "food_items")
@SuppressWarnings("serial")
public class FoodItem extends PerishableResource {

    /**
     * Allergens the item contains; empty when it contains none of them.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "food_item_allergens", joinColumns = @JoinColumn(name = "food_item_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "allergen", nullable = false, length = 20)
    private Set<Allergen> allergens = new HashSet<>();

    /**
     * Temperature range the item must be kept in.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "storage_temperature", nullable = false, length = 20)
    private StorageTemperature storageTemperature;

    /**
     * Whether the item is a surprise bag (assorted contents, fixed discount).
     */
    @Column(name = "surprise_bag", nullable = false)
    private boolean surpriseBag;

    /**
     * Default constructor.
     */
    public FoodItem() {
        super();
    }

    /**
     * Returns a builder for the common resource attributes of a food item.
     *
     * @return a new builder
     */
    public static ResourceBuilder<FoodItem> builder() {
        return new ResourceBuilder<>(FoodItem::new);
    }

    /**
     * A food item can be rescued until the offerer closes today, or until the
     * end of its expiration date if that comes first.
     *
     * @param now the current date and time
     * @return the critical deadline
     */
    @Override
    public LocalDateTime criticalDeadline(LocalDateTime now) {
        LocalDateTime closing = now.toLocalDate().atTime(getOfferer().getClosingTime());
        if (getExpirationDate() == null) {
            return closing;
        }
        LocalDateTime endOfExpirationDate = getExpirationDate().atTime(LocalTime.MAX);
        return endOfExpirationDate.isBefore(closing) ? endOfExpirationDate : closing;
    }

    public Set<Allergen> getAllergens() {
        return allergens;
    }

    public void setAllergens(Set<Allergen> allergens) {
        this.allergens = allergens == null || allergens.isEmpty()
                ? new HashSet<>()
                : new HashSet<>(EnumSet.copyOf(allergens));
    }

    public StorageTemperature getStorageTemperature() {
        return storageTemperature;
    }

    public void setStorageTemperature(StorageTemperature storageTemperature) {
        this.storageTemperature = storageTemperature;
    }

    public boolean isSurpriseBag() {
        return surpriseBag;
    }

    public void setSurpriseBag(boolean surpriseBag) {
        this.surpriseBag = surpriseBag;
    }

    @Override
    public String toString() {
        return "FoodItem [id=" + getId() + ", name=" + getName() + ", surpriseBag=" + surpriseBag + "]";
    }
}
