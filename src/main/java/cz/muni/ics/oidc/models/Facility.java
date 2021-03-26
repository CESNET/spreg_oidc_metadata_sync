package cz.muni.ics.oidc.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.StringUtils;

/**
 * Facility object model.
 *
 * @author Dominik Frantisek Bucik <bucik@.ics.muni.cz>
 * @author Ondrej Ernst <ondra.ernst@gmail.com>
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class Facility {

    @NonNull private Long id;
    @NonNull private String name;
    @NonNull private String description = "";

    public Facility(Long id, String name, String description) {
        this.setId(id);
        this.setName(name);
        this.setDescription(description);
    }

    public void setName(@NonNull String name) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("name cannot be empty");
        }

        this.name = name;
    }

    public Object copy() {
        return new Facility(id, name, description);
    }

}
