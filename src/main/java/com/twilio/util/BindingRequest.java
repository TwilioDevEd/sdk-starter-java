package com.twilio.util;

import com.google.gson.annotations.SerializedName;

public class BindingRequest {

    @SerializedName("endpoint")
    String endpoint;
    @SerializedName("BindingType")
    String bindingType;
    @SerializedName("Address")
    String address;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBindingType() {
        return bindingType;
    }

    public void setBindingType(String bindingType) {
        this.bindingType = bindingType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public BindingRequest(String endpoint, String bindingType, String address) {

        this.endpoint = endpoint;
        this.bindingType = bindingType;
        this.address = address;
    }

    public BindingRequest() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BindingRequest that = (BindingRequest) o;

        if (endpoint != null ? !endpoint.equals(that.endpoint) : that.endpoint != null)
            return false;
        if (bindingType != null ? !bindingType.equals(that.bindingType) : that.bindingType != null)
            return false;
        return address != null ? address.equals(that.address) : that.address == null;
    }

    @Override
    public int hashCode() {
        int result = endpoint != null ? endpoint.hashCode() : 0;
        result = 31 * result + (bindingType != null ? bindingType.hashCode() : 0);
        result = 31 * result + (address != null ? address.hashCode() : 0);
        return result;
    }
}
