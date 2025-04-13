import argparse
import datetime
import os
import subprocess

import boto3
import yaml

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
port = cfg['port']

remote_mongodumper_command_prefix = ["ssh", "-p", str(port), host,
                                     "java", "-Xmx32m", "-jar",
                                     "./mongodbdumper.jar",
                                     ] + [e for coll in cfg['mongohosts'] for e in ["--host", coll]]

from_m = datetime.datetime.strptime(getattr(args, "from"), '%Y-%m')
to_m = datetime.datetime.strptime(args.to, '%Y-%m')

dump_location = args.dump_path

session = boto3.session.Session()
s3 = session.client(service_name='s3', endpoint_url='https://storage.yandexcloud.net')

for start, end in monthranges(from_m, to_m):

    time_query_str = "'{time:{\'\$gte\':{\'\$date\':\"%sZ\"},\'\$lte\':{\'\$date\':\"%sZ\"}}}'" \
                     % (start.isoformat(), end.isoformat())

    print(time_query_str)

    # Mongo dumping monthly

    for db in cfg['mongodbs']:
        os.makedirs(os.path.join(dump_location, db, "packages-monthly"), exist_ok=True)

        command = remote_mongodumper_command_prefix + ["-d", db, "-p", "objPacks.o.*", "-q", time_query_str, "-zip", "-v"]
        place = f'packages-monthly/{db}/{start.year}/{start.month:#02}.zip'
        file = os.path.join(dump_location, os.path.join(db, "packages-monthly", "packs-dump-%s.zip" % start.strftime("%Y-%m")))

        print(command, "to", file)

        with open(file, "w") as dumpfile:
             subprocess.run(command, stdout=dumpfile)

        s3.upload_file(file, 'wrc-backup-storage', place)

        with open(file + ".sha256", "w") as sumfile:
            sumfile.write(sha256_checksum(file))

        os.remove(file)
        s3.upload_file(file + ".sha256", 'wrc-backup-storage', place + ".sha256")
