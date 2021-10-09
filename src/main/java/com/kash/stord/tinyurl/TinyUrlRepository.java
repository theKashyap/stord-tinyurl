package com.kash.stord.tinyurl;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TinyUrlRepository extends CrudRepository<UrlMapping, Long> {

}
