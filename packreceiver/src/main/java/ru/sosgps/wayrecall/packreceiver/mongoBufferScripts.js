function biggestBuffer() {
    var big = null;
    var collections = db.getCollectionNames();

    for (var i = 0; i < collections.length; i++) {
        if (collections[i].indexOf("objPacks.buffer") == 0) {

            if (big == null || big.count() < db.getCollection(collections[i]).count()) {
                big = db.getCollection(collections[i])
            }
        }
    }
    return big;
}


biggestBuffer().aggregate(
    [
        {
            $group: {
                _id: "$uid",
                count: {$sum: 1}
            }
        },
        {$sort: {count: -1}}
    ]
);