find ~/inclog  -type f -mtime +3 -exec rm {} \;
find ~/zipdumps/  -type f -mtime +3 -exec rm {} \;
find ~/sessionLog/ -type f -mtime +3 -exec rm {} \;
df -h
