#!/bin/bash
# register in cron using 0 8 * * * sudo -u postgres /path/to/script/emailHolidayScript-staging.sh >/dev/null 2>&1
# Runs Campaign Update Queries then sends status email to targetEmails.

# EmailAddresses to send to
targetEmails="billy@siliconmtn.com,mikedownard@gmail.com"

# Build Email Text 
email="Results of holiday Email Campaign Script-Staging on $(date)."

# Run Queries to Modify Email Campaign Data Source and Approval Flags
email="$email\nDisable Daily Campaign `psql -d raptor_smarttrak_2 -U postgres -c "update core.email_campaign_instance set approval_flg = 0 where campaign_instance_id = '8b8e77d02ff7e070c0a802378321dddf' and current_date in ('2018-12-10', '2018-12-17');"`"
email="$email\nUpdate and Set for Christmas `psql -d raptor_smarttrak_2 -U postgres -c "update core.email_campaign_instance set approval_flg = 1, campaign_source_id = 'SMARTTRAK_CHRISTMAS' where campaign_instance_id ='8b8e77d02ff7e070c0a802378321dddf' and current_date = '2018-12-11';"`"
email="$email\nUpdate and Set for New Years `psql -d raptor_smarttrak_2 -U postgres -c "update core.email_campaign_instance set approval_flg = 1, campaign_source_id = 'SMARTTRAK_NEW_YEARS' where campaign_instance_id ='8b8e77d02ff7e070c0a802378321dddf' and current_date = '2019-01-18';"`"
email="$email\nReset to Normal `psql -d raptor_smarttrak_2 -U postgres -c "update core.email_campaign_instance set approval_flg = 1, campaign_source_id = '925a6a5a15b81012c0a8023747623d18' where campaign_instance_id ='8b8e77d02ff7e070c0a802378321dddf' and current_date in('2018-12-12', '2019-12-19');"`"

# Send Emails
printf "$email" | /usr/bin/mail -s "EC Holiday Script" "$targetEmails"
