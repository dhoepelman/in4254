SELECT
    ord,
    t.expected,
    ROUND(t.correct*100.0/t.total,1) AS correct
FROM (
    SELECT
	expected,
	COUNT(*) AS total,
	COUNT(NULLIF(found=expected, 0)) AS correct
    FROM localizationofflineprocessingresult
    WHERE process_id = 4
    GROUP BY expected
    ) AS t
JOIN roomordering ON t.expected = roomordering.r
ORDER BY ord