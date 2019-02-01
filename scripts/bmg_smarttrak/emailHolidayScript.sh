#!/bin/bash
# A Cron needs registered for these changes.  Details about cron run are below.
#
# Install by 8am 12/21/2018.
# Script to be run from 12/21/2018 - 1/3/2019, running at 8am each day.
# Uninstall date of 1/2/2019 after 8am.
# register in cron using 
# 0 8 * * * sudo -u postgres /path/to/emailHolidayScript.sh >/dev/null 2>&1
#
# Runs Campaign Update Queries then sends status email to targetEmails.

# EmailAddresses to send to
targetEmails="billy@siliconmtn.com,mikedownard@gmail.com,jmckain@siliconmtn.com"

# Build Email Text 
email="Results of holiday Email Campaign Script on $(date)."

# Run Queries to Modify Email Campaign Data Source and Approval Flags

# Disable Email Campaigns 12/21/2018 and 12/31/2018
email="$email\nDisable Daily Campaign `psql -d webcrescendo_sb -U postgres -c "update core.email_campaign_instance set approval_flg = 0 where campaign_instance_id = '8b8e77d02ff7e070c0a802378321dddf' and current_date in ('2018-12-21', '2018-12-31');"`"

# Enable Christmas Campaign 12/25/2018 to ensure 12/26/2018 send is set up correctly
email="$email\nUpdate and Set for Christmas `psql -d webcrescendo_sb -U postgres -c "update core.email_campaign_instance set approval_flg = 1, campaign_source_id = 'SMARTTRAK_CHRISTMAS' where campaign_instance_id ='8b8e77d02ff7e070c0a802378321dddf' and current_date = '2018-12-25';"`"

# Enable New Years Campaign 1/1/2019 to ensure 1/2/2019 send is set up correctly
email="$email\nUpdate and Set for New Years `psql -d webcrescendo_sb -U postgres -c "update core.email_campaign_instance set approval_flg = 1, campaign_source_id = 'SMARTTRAK_NEW_YEARS' where campaign_instance_id ='8b8e77d02ff7e070c0a802378321dddf' and current_date = '2019-01-01';"`"

# Reset Campaigns to normal on 12/26/2018 and 1/2/2018 to ensure normal campaign is run. 
email="$email\nReset to Normal `psql -d webcrescendo_sb -U postgres -c "update core.email_campaign_instance set approval_flg = 1, campaign_source_id = '925a6a5a15b81012c0a8023747623d18' where campaign_instance_id ='8b8e77d02ff7e070c0a802378321dddf' and current_date in('2018-12-26', '2019-01-02');"`"

# Send Emails
printf "$email" | /usr/bin/mail -s "EC Holiday Script" "$targetEmails"
