import argparse
import datetime
import os
import subprocess

import boto3
import yaml

from utils import sha256_checksum, dayranges

data_collections = [
    "accounts", "objects", "equipments", "equipmentTypes", "eqhistory",
    "events", "tariffs", "users", "usersPermissions", "balanceHistory", "balanceHistoryWithDetails",
    "billingPermissions", "billingRoles", "groupsOfObjects",
    "notificationRules", "notificationRulesStates", "smses", "domainEvents", "snapshotEvents"]

parser = argparse.ArgumentParser()
parser.add_argument("--config", help="path to \"backupper.yaml\" config file", default="/etc/backupper.yaml")
parser.add_argument("--dump-path", help="path to put dumps", default="/var/opt/backupper")
parser.add_argument("--day", help="day to dump packages 2020-07-15", default=None)
parser.add_argument("--from", help="day in format 2020-07-15", default=None)
parser.add_argument("--to", help="day in format 2020-07-15 (not inclusive)", default=None)
args = parser.parse_args()

with open(args.config, 'r') as cfg_file:
    cfg = yaml.safe_load(cfg_file)

host = cfg['host']
port = cfg.get('port', 22)

remote_mongodumper_command_prefix = ["ssh", "-p", str(port), host,
                                     "java", "-Xmx32m", "-jar",
                                     "./mongodbdumper.jar", "--primary"
                                     ] + [e for coll in cfg['mongohosts'] for e in ["--host", coll]]

drange = None

if getattr(args, "from"):
    from_d = datetime.datetime.strptime(getattr(args, "from"), '%Y-%m-%d')
    to_d = datetime.datetime.strptime(args.to, '%Y-%m-%d')
    drange = dayranges(from_d, to_d)
elif args.day:
    today = datetime.datetime.strptime(getattr(args, "day"), '%Y-%m-%d')
    yesterday = today
    yesterdaystart = yesterday - datetime.timedelta(days=1)
    drange = [(yesterdaystart, yesterday)]
else:
    today = datetime.datetime.combine(datetime.date.today(), datetime.time(0, 0, 0))
    yesterday = today - datetime.timedelta(days=1)
    yesterdaystart = yesterday - datetime.timedelta(days=1)
    drange = [(yesterdaystart, yesterday)]

dump_location = args.dump_path

session = boto3.session.Session()
s3 = session.client(service_name='s3', endpoint_url='https://storage.yandexcloud.net')

# Mongo dumping

for yesterdaystart, yesterday in drange:
    time_query_str = "'{time:{\'\$gte\':{\'\$date\':\"%sZ\"},\'\$lte\':{\'\$date\':\"%sZ\"}}}'" \
                     % (yesterdaystart.isoformat(), yesterday.isoformat())

    print(time_query_str)

    for db in cfg['mongodbs']:
        os.makedirs(os.path.join(dump_location, db, "packages"), exist_ok=True)

        if (not args.day) and (not getattr(args, "from")):
            file = os.path.join(dump_location, os.path.join(db, "data-dump-%s.zip" % yesterday.date().isoformat()))
            with open(file, "w") as dumpfile:
                cmd = remote_mongodumper_command_prefix + ["-d", db] + \
                      [e for coll in data_collections for e in ["-c", coll]] + ["-zip", "-v"]
                subprocess.run(cmd, stdout=dumpfile)

            s3.upload_file(file, 'wrc-backup-storage',
                           f'data-dump/{db}/{yesterday.year}/{yesterday.month:#02}/{yesterday.day:#02}.zip')
            os.remove(file)

        place = f'packages-daily/{db}/{yesterday.year}/{yesterday.month:#02}/{yesterday.day:#02}.zip'
        file = os.path.join(dump_location, os.path.join(db, "packages", "packs-dump-%s.zip" % yesterday.date().isoformat()))
        with open(file, "w") as dumpfile:
            subprocess.run(remote_mongodumper_command_prefix + [
                "-d", db,
                "-p", "objPacks.*",
                "-q", time_query_str,
                "-zip", "-v"], stdout=dumpfile)

        s3.upload_file(file, 'wrc-backup-storage', place)
        with open(file + ".sha256", "w") as sumfile:
            sumfile.write(sha256_checksum(file))
        os.remove(file)
        s3.upload_file(file + ".sha256", 'wrc-backup-storage', place + ".sha256")

if (not args.day) and (not getattr(args, "from")):

    today = datetime.datetime.combine(datetime.date.today(), datetime.time(0, 0, 0))

    # Postgres dumping

    os.makedirs(dump_location, exist_ok=True)

    file = os.path.join(dump_location, "seniel-pg-dump-%s.sql.xz" % today.date().isoformat())
    with open(file, "w") as dumpfile:
        subprocess.run([
            "ssh", host, "-p", str(port),
            "pg_dump", "-a", "--no-owner", "-h", "localhost", "-p", "6543", "-U", cfg['postgres']['user'], "seniel-pg", "|",  "xz -1"
            ], input=cfg['postgres']['password'], encoding='ascii', stdout=dumpfile)

    s3.upload_file(file, 'wrc-backup-storage', f'seniel-pg-dump/{today.year}/{today.month:#02}/{today.day:#02}.sql.xz')
    os.remove(file)

    # Instances config dump

    for inst in cfg['wrc_instances']:
        os.makedirs(os.path.join(dump_location, "instances-configs", inst), exist_ok=True)
        file = os.path.join(dump_location, os.path.join("instances-configs", inst, "%s-%s.txz" % (inst, today.date().isoformat())))
        with open(file, "w") as dumpfile:
            conf_path = (cfg['wrc_home'] + "/conf/wrcinstances/").replace('//', '/')
            subprocess.run([
                "ssh", host, "-p", str(port),
                "tar",  "-c",
                "-C", conf_path,
                inst, "|", "xz"
                ], stdout=dumpfile)
        s3.upload_file(file, 'wrc-backup-storage', f'instances-configs/{inst}/{today.year}/{today.month:#02}/{today.day:#02}.txz')
        os.remove(file)
