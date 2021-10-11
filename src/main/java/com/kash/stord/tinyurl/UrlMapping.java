package com.kash.stord.tinyurl;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * A simple DB Entity. Represents a mapping between a numeric id and a long URL.
 */
@Entity
public class UrlMapping {

    private @Id
    @GeneratedValue
    Long id;

    private String longUrl;

    protected UrlMapping() {
    }

    public UrlMapping(String longUrl) {
        this.longUrl = longUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UrlMapping employee = (UrlMapping) o;
        return Objects.equals(id, employee.id) && Objects.equals(longUrl, employee.longUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, longUrl);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public void setLongUrl(String originalUrl) {
        this.longUrl = originalUrl;
    }

    @Override
    public String toString() {
        // FIXME: JSON is more parsable. Helps extract info easily from logs for
        //        post-processing, create alerts, build dashboards etc.
        return String.format("{\"id\": \"%d\", \"originalUrl\": \"%s\"}", id, longUrl);
    }
}
