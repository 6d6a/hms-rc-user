DROP TABLE IF EXISTS records;
CREATE TABLE records (
  id int(11) NOT NULL AUTO_INCREMENT,
  domain_id int(11) DEFAULT NULL,
  name varchar(255) DEFAULT NULL,
  type varchar(6) DEFAULT NULL,
  content varchar(512) DEFAULT NULL,
  ttl int(11) DEFAULT NULL,
  prio int(11) DEFAULT NULL,
  change_date int(11) DEFAULT NULL,
  active varchar(1) NOT NULL DEFAULT '1' check (active in ('1', '0')),
  PRIMARY KEY (id)
);

DROP TABLE IF EXISTS domains;
CREATE TABLE domains (
  id int(11) NOT NULL AUTO_INCREMENT,
  name varchar(255) NOT NULL DEFAULT '',
  master varchar(20) DEFAULT NULL,
  last_check int(11) DEFAULT NULL,
  type varchar(6) NOT NULL DEFAULT '',
  notified_serial int(11) DEFAULT NULL,
  account varchar(40) DEFAULT NULL,
  active varchar(1) NOT NULL DEFAULT '1' check (active in ('1', '0')),
  uid int(10) NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);

INSERT INTO domains VALUES('1', 'majordomo.ru', null, null, '', null, null, '1', 2000);
INSERT INTO records VALUES('1', '1', 'majordomo.ru', 'SOA', 'ns.majordomo.ru. support.majordomo.ru. 2004032900 3600 900 3600000 3600', '3600', '10', null, '1');
INSERT INTO records VALUES('2', '1', 'majordomo.ru', 'A', '8.8.8.8', '3600', null, null, '1');
INSERT INTO records VALUES('3', '1', 'majordomo.ru', 'MX', 'mail.majordomo.ru', '3600', '10', null, '1');
INSERT INTO records VALUES('4', '1', 'majordomo.ru', 'NS', 'ns.majordomo.ru', '3600', '10', null, '1');
INSERT INTO records VALUES('5', '1', 'majordomo.ru', 'NS', 'ns2.majordomo.ru', '3600', '10', null, '1');
INSERT INTO records VALUES('6', '1', 'majordomo.ru', 'NS', 'ns3.majordomo.ru', '3600', '10', null, '1');
INSERT INTO records VALUES('7', '1', 'sub.majordomo.ru', 'A', '8.8.8.8', '3600', null, null, '1');