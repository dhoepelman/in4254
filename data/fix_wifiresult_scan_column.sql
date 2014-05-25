UPDATE wifiresult
SET scan =
    (SELECT wrc.id
    FROM wifiresultcollection AS wrc
    WHERE
        wrc.timestamp = wifiresult.timestamp
        AND wrc.room = wifiresult.room
    )