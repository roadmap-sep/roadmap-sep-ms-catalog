package sh.roadmap.sep.catalog.infrastructure.output.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "category", uniqueConstraints = {@UniqueConstraint(name = "uk_category_slug", columnNames = "slug")})
public class CategoryEntity implements Persistable<Long> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 150)
    private String slug;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false)
    private boolean active;

    @Override
    public Long getId() {
        return id;
    }

    @Transient
    @Override
    public boolean isNew() {
        return null == getId();
    }
}
