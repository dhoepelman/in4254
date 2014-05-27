SELECT process_id, iterations, COUNT(*)
FROM localizationofflineprocessingresult
WHERE process_id BETWEEN 3 AND 4
AND expected = found
GROUP BY process_id,iterations