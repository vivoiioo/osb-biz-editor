#!/bin/bash

#set CLASSPATH

MW_HOME=/opt/web/wl/osb11.1.1.7
OSB_HOME=${MW_HOME}/osb11

classpath=${MW_HOME}/domains/OSB_VIVO360_1_PROD/scripts/teste/bin:${MW_HOME}/modules/com.bea.core.management.jmx_1.4.2.0.jar:${MW_HOME}/wlserver_10.3/server/lib/weblogic.jar:${OSB_HOME}/lib/modules/com.bea.alsb.utils.jar:${OSB_HOME}/lib/modules/com.bea.alsb.security.api.jar:${OSB_HOME}/lib/modules/com.bea.alsb.resources.globalsettings.jar:${OSB_HOME}/lib/sb-kernel-api.jar:${OSB_HOME}/lib/sb-kernel-common.jar:${OSB_HOME}/lib/sb-kernel-resources.jar:${OSB_HOME}/lib/sb-kernel-impl.jar:${OSB_HOME}/lib/sb-transports-main.jar:${OSB_HOME}/modules/com.bea.common.configfwk_1.7.0.0.jar:${OSB_HOME}/modules/com.bea.alsb.statistics_1.4.0.0.jar:${OSB_HOME}/lib/modules/com.bea.alsb.resources.core.jar

echo $classpath

sessao=bizWM
host=brtlvlts0252sl
port=7000
username=admWLS
password=0s6Vivo360##Pr0d
workmanager=BusinessServices.WorkManager
connectionTimeout=5
operacaoTimeout=36000000
data=$(date '+%Y%m%d%H%M%S')

##################################
#       tipos de operacoes       #
#   workmanager = 1              #
#   connectionTimeout = 2        #
#   All = 3                      #
##################################

operacao=3

java -cp ${classpath} -Dweblogic.MaxMessageSize=30000000 ChangeBusinessService ${sessao} ${host} ${port} ${username} ${password} ${workmanager} ${connectionTimeout} ${operacao} ${operacaoTimeout} > ./saidaScript.${data}.out
