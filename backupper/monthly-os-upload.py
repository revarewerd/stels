import argparse
import datetime
import os
import yaml
import boto3

from utils import monthranges, sha256_checksum

parser = argparse.ArgumentParser()
parser.add_argument("--config", help="path to \"backupper.yaml\" config file", default="/etc/backupper.yaml")
parser.add_argument("--dump-path", help="path to put dumps", default="/var/opt/backupper")
parser.add_argument("--from", help="month in format 2020-01")
parser.add_argument("--to", help="month in format 2020-02 (not inclusive)")
args = parser.parse_args()

with open(args.config, 'r') as cfg_file:
    cfg = yaml.safe_load(cfg_file)

host = cfg['host']

# from_m = datetime.datetime.strptime(getattr(args, "from"), '%Y-%m')
# to_m = datetime.datetime.strptime(args.to, '%Y-%m')

dump_location = args.dump_path

session = boto3.session.Session()
s3 = session.client(service_name='s3',endpoint_url='https://storage.yandexcloud.net')


for db in cfg['mongodbs']:

    dir = os.path.join(dump_location, db, "packages")
    for file in os.listdir(dir):
        place = os.path.join(db, "packages", file)
        file = os.path.join(dump_location, place)
        print(file, 'wrc-backup-storage', place)
        s3.upload_file(file, 'wrc-backup-storage', place)
        with open(file + ".sha256", "w") as sumfile:
            sumfile.write(sha256_checksum(file))
        s3.upload_file(file + ".sha256", 'wrc-backup-storage', place + ".sha256")
