CREATE DOMAIN IF NOT EXISTS enum as int(1);

CREATE TABLE records (
  id int(11) NOT NULL AUTO_INCREMENT,
  domain_id int(11) DEFAULT NULL,
  name varchar(255) DEFAULT NULL,
  type varchar(6) DEFAULT NULL,
  content varchar(512) DEFAULT NULL,
  ttl int(11) DEFAULT NULL,
  prio int(11) DEFAULT NULL,
  change_date int(11) DEFAULT NULL,
  active enum NOT NULL DEFAULT '1',
  PRIMARY KEY (id)
);

CREATE TABLE domains (
  id int(11) NOT NULL AUTO_INCREMENT,
  name varchar(255) NOT NULL DEFAULT '',
  master varchar(20) DEFAULT NULL,
  last_check int(11) DEFAULT NULL,
  type varchar(6) NOT NULL DEFAULT '',
  notified_serial int(11) DEFAULT NULL,
  account varchar(40) DEFAULT NULL,
  active enum NOT NULL DEFAULT '1',
  uid int(10) NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);

INSERT INTO domains VALUES('1', 'example.com', null, null, '', null, null, '1', 2000);
INSERT INTO records VALUES('1', '1', 'example.com', 'SOA', 'ns.majordomo.ru. support.majordomo.ru. 2004032900 3600 900 3600000 3600', '3600', '10', null, '1');
INSERT INTO records VALUES('2', '1', 'example.com', 'A', '8.8.8.8', '3600', null, null, '1');
INSERT INTO records VALUES('3', '1', 'example.com', 'MX', 'mail.majordomo.ru', '3600', '10', null, '1');
INSERT INTO records VALUES('4', '1', 'example.com', 'NS', 'ns.majordomo.ru', '3600', '10', null, '1');
INSERT INTO records VALUES('5', '1', 'example.com', 'NS', 'ns2.majordomo.ru', '3600', '10', null, '1');
INSERT INTO records VALUES('6', '1', 'example.com', 'NS', 'ns3.majordomo.ru', '3600', '10', null, '1');
INSERT INTO records VALUES('7', '1', 'sub.example.com', 'A', '8.8.8.8', '3600', null, null, '1');