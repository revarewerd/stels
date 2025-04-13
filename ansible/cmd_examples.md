time ansible-playbook -i inventrories/ubuntu-virt/inventory.yaml  playbook.yaml -l wrc_server --skip-tags "initialisation"

ansible -i inventrories/ubuntu-virt/inventory.yaml -m shell -a "shutdown now" all

time ansible-playbook -i inventrories/prod/prod/inventory.yaml backupper-playbook.yaml -l backupper

time ansible-playbook -i inventrories/prod/prod/inventory.yaml  playbook.yaml -l wrc_server --tags "deploy"

yc compute instance create --ssh-key ~/.ssh/id_rsa.pub \
--create-boot-disk image-folder-id=standard-images,image-family=ubuntu-2004-lts,size=100G,type=network-ssd \
--memory 8G --cores 2 --core-fraction 50 --preemptible \
--public-ip --async \
--name wrc-dev1

cd .. && JAVA_HOME=$(/usr/libexec/java_home -v 1.8); mvn install -DskipTests -pl modules/m2msms -pl monitoring && cd ansible/ && time ansible-playbook -i inventrories/prod/staging/inventory.yaml  playbook.yaml -l wrc_server --tags "deploy"