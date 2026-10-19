package testdata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/** A catalog product and its expected cart behaviour, resolved from testdata/{env}/products.csv by key. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Product {
    private String name;
    private String expectedCartText;
    private String expectedTotal;
}
