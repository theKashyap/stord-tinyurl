package com.kash.stord.tinyurl;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Interface to DB, given the simple usecases, we don't need to override
 * default implementation injected by Spring
 */
@Repository
public interface TinyUrlRepository extends CrudRepository<UrlMapping, Long> {

}
