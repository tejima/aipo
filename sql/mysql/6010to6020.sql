-- 20110905
-- modify table struct
ALTER TABLE `eip_t_schedule` ADD `mail_flag` CHAR(1) ;
ALTER TABLE `eip_t_cabinet_file` ADD `counter` INTEGER ;

-- update data
UPDATE `eip_t_schedule` SET `mail_flag` = 'A' ;
UPDATE `eip_m_addressbook_company` SET `company_name` = '', `company_name_kana` = '' WHERE `company_id` = 1;
-- 20110905

--- 20110909
--- create new table
CREATE TABLE eip_m_facility_group
(
    `group_id` int(11) NOT NULL AUTO_INCREMENT,
    `group_name` varchar (64) COLLATE utf8_unicode_ci,
    PRIMARY KEY (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE eip_m_facility_group_map
(
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `facility_id` int(11),
    `group_id` int(11),
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
--- 20110909