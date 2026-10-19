package testdata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/** CRUD payload fields for the /objects API, resolved from testdata/{env}/objects.xlsx by key. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectTestData {
    private String name;
    private Integer year;
    private Double price;
    private String cpuModel;
    private String hardDiskSize;
    private String color;

    /** Builds the request "data" map, omitting fields left blank in the spreadsheet row. */
    public Map<String, Object> toDataMap() {
        Map<String, Object> data = new LinkedHashMap<>();
        if (year != null) {
            data.put("year", year);
        }
        if (price != null) {
            data.put("price", price);
        }
        if (cpuModel != null && !cpuModel.isBlank()) {
            data.put("CPU model", cpuModel);
        }
        if (hardDiskSize != null && !hardDiskSize.isBlank()) {
            data.put("Hard disk size", hardDiskSize);
        }
        if (color != null && !color.isBlank()) {
            data.put("color", color);
        }
        return data;
    }
}
