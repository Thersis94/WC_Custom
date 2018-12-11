#!/bin/bash
email="Results of holiday Email Campaign Script on $(date)."

targetEmails="billy@siliconmtn.com"
email="$email\nDisable Daily Campaign `psql -d raptor_smarttrak_2 -U postgres -c "update core.email_campaign_instance set approval_flg = 0 where campaign_instance_id = '8b8e77d02ff7e070c0a802378321dddf' and current_date in ('2018-12-21', '2018-12-31');"`"
email="$email\nUpdate and Set for Christmas `psql -d raptor_smarttrak_2 -U postgres -c "update core.email_campaign_instance set approval_flg = 1, campaign_source_id = 'SMARTTRAK_CHRISTMAS' where campaign_instance_id ='8b8e77d02ff7e070c0a802378321dddf' and current_date = '2018-12-25';"`"
email="$email\nUpdate and Set for New Years `psql -d raptor_smarttrak_2 -U postgres -c "update core.email_campaign_instance set approval_flg = 1, campaign_source_id = 'SMARTTRAK_NEW_YEARS' where campaign_instance_id ='8b8e77d02ff7e070c0a802378321dddf' and current_date = '2019-01-01';"`"
email="$email\nReset to Normal `psql -d raptor_smarttrak_2 -U postgres -c "update core.email_campaign_instance set approval_flg = 1, campaign_source_id = '925a6a5a15b81012c0a8023747623d18' where campaign_instance_id ='8b8e77d02ff7e070c0a802378321dddf' and current_date in('2018-12-26', '2019-01-02');"`"

printf "$email" | /usr/bin/mail -s "EC Holiday Script" "$targetEmails"
