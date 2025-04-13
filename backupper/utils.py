import calendar
import datetime
import hashlib


def add_months(sourcedate, months):
    month = sourcedate.month - 1 + months
    year = sourcedate.year + month // 12
    month = month % 12 + 1
    day = min(sourcedate.day, calendar.monthrange(year, month)[1])
    return datetime.date(year, month, day)


def monthranges(from_m, to_m):
    cur = from_m
    while cur < to_m:
        n = datetime.datetime.combine(add_months(cur, 1), datetime.time(0, 0, 0))
        yield cur, n
        cur = n


def dayranges(from_d, to_d):
    cur = from_d
    while cur < to_d:
        n = datetime.datetime.combine(cur + datetime.timedelta(days=1), datetime.time(0, 0, 0))
        yield cur, n
        cur = n


def sha256_checksum(filename, block_size=65536):
    sha256 = hashlib.sha256()
    with open(filename, 'rb') as f:
        for block in iter(lambda: f.read(block_size), b''):
            sha256.update(block)
    return sha256.hexdigest()
