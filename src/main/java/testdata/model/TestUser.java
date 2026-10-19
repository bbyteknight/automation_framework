package testdata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/** A named login identity, resolved from testdata/{env}/users.json by key (e.g. "standardUser"). */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestUser {
    private String username;
    private String password;
}
