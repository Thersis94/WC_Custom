#!/bin/bash
# Install by 8am 12/14/2018.
# Script to be run from 12/14/2018 - 12/26/2018, running at 8am each day.
# Uninstall date of 12/26 after 8am.
# register in cron using
# 0 8 * * * sudo -u postgres /path/to/emailHolidayScript-staging.sh >/dev/null 2>&1
# Runs Campaign Update Queries then sends status email to targetEmails.

# EmailAddresses to send to
targetEmails="billy@siliconmtn.com,mikedownard@gmail.com,jmckain@siliconmtn.com"

# Build Email Text 
email="Results of holiday Email Campaign Script-Staging on $(date)."

# Run Queries to Modify Email Campaign Data Source and Approval Flags

# Disable Staging Email Campaigns 12/14/2018 and 12/24/2018 for 1 week prior test of next weeks email system.
email="$email\nDisable Daily Campaign `psql -d webcrescendo_sb -U postgres -c "update core.email_campaign_instance set approval_flg = 0 where campaign_instance_id = '8b8e77d02ff7e070c0a802378321dddf' and current_date in ('2018-12-14', '2018-12-24');"`"

# Enable Christmas Campaign in staging 12/18/2018 to be sent on 12/19/2018
email="$email\nUpdate and Set for Christmas `psql -d webcrescendo_sb -U postgres -c "update core.email_campaign_instance set approval_flg = 1, campaign_source_id = 'SMARTTRAK_CHRISTMAS' where campaign_instance_id ='8b8e77d02ff7e070c0a802378321dddf' and current_date = '2018-12-18';"`"

# Enable New Years Campaign in staging 12/25/2018 to be sent on 12/26/2018
email="$email\nUpdate and Set for New Years `psql -d webcrescendo_sb -U postgres -c "update core.email_campaign_instance set approval_flg = 1, campaign_source_id = 'SMARTTRAK_NEW_YEARS' where campaign_instance_id ='8b8e77d02ff7e070c0a802378321dddf' and current_date = '2018-12-25';"`"

# Resume Normal Campaign behavior in staging 12/19/2018 and 12/26/2018
email="$email\nReset to Normal `psql -d webcrescendo_sb -U postgres -c "update core.email_campaign_instance set approval_flg = 1, campaign_source_id = '925a6a5a15b81012c0a8023747623d18' where campaign_instance_id ='8b8e77d02ff7e070c0a802378321dddf' and current_date in('2018-12-19', '2019-12-26');"`"

# Send Emails
printf "$email" | /usr/bin/mail -s "EC Holiday Script" "$targetEmails"
