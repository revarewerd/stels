import argparse
import datetime
import os
import subprocess

import boto3
import yaml

from utils import sha256_checksum

parser = argparse.ArgumentParser()
parser.add_argument("--config", help="path to \"backupper.yaml\" config file", default="/etc/backupper.yaml")
parser.add_argument("--dump-path", help="path to put dumps", default="/var/backupper_dedicated_disk")
args = parser.parse_args()

with open(args.config, 'r') as cfg_file:
    cfg = yaml.safe_load(cfg_file)

host = cfg['host']
port = cfg['port']

dump_location = args.dump_path
today = datetime.datetime.combine(datetime.date.today(), datetime.time(0, 0, 0))

session = boto3.session.Session()
s3 = session.client(service_name='s3', endpoint_url='https://storage.yandexcloud.net')

# nominatim dumping

os.makedirs(dump_location, exist_ok=True)

place = "nominatim-pg-dump-%s.sql.xz" % today.date().isoformat()
file = os.path.join(dump_location, place)
with open(file, "w") as dumpfile:
    subprocess.run([
        "ssh", "-p", str(port), host,
        "pg_dump", "-a", "--no-owner", "-h", cfg['nominatim']['host'], "-p", "5432", "-U", cfg['nominatim']['user'], "nominatim", "|",  "xz -1"
        ], input=cfg['nominatim']['password'], encoding='ascii', stdout=dumpfile)

s3.upload_file(file, 'wrc-backup-storage', place)
os.remove(file)

