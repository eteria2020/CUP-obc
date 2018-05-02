package eu.philcar.csg.OBC.data.model;

import eu.philcar.csg.OBC.data.datasources.base.BaseResponse;

/**
 * Created by Fulvio on 13/04/2018.
 */

public class ModelResponse extends BaseResponse {
    private String model;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public ModelResponse(String model) {
        this.model = model;
    }

    @Override
    public String toString() {
        return "ModelResponse{" +
                "model='" + model + '\'' +
                '}';
    }
}
