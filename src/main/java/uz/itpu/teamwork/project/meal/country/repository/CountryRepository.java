package uz.itpu.teamwork.project.meal.country.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.itpu.teamwork.project.meal.country.model.Country;

import java.util.List;
import java.util.Optional;

public interface CountryRepository extends JpaRepository<Country, Long> {

    @Query("SELECT c FROM Country c WHERE UPPER(c.code) = UPPER(:code)")
    Optional<Country> findByCode(@Param("code") String code);

    List<Country> findByIsActiveTrue();

    @Query("SELECT COUNT(c) > 0 FROM Country c WHERE UPPER(c.code) = UPPER(:code)")
    boolean existsByCode(@Param("code") String code);
}