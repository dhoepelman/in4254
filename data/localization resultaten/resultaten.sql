SELECT
        t.process_id,
        t.total,
        t.correct,
        t.incorrect,
        t.correct_including_adjacent,
	ROUND(t.correct*100.0/t.total) AS pcorrect,
	ROUND(t.incorrect*100.0/t.total) AS pincorrect,
	ROUND(t.correct_including_adjacent*100.0/t.total) AS padjacent
FROM (
    SELECT
	process_id,
	COUNT(*) AS total,
	COUNT(NULLIF(found=expected, 0)) AS correct,
	COUNT(NULLIF(found=expected, 1)) AS incorrect,
	COUNT(NULLIF(
		found=expected OR found IN (SELECT adj FROM adjacent WHERE room = expected)
		,0)) AS correct_including_adjacent
    FROM localizationofflineprocessingresult
    GROUP BY process_id
    ) AS t