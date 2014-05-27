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