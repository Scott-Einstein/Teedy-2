create memory table T_USER_REGISTRATION_REQUEST (
  URR_ID_C varchar(36) not null,
  URR_USERNAME_C varchar(50) not null,
  URR_EMAIL_C varchar(100) not null,
  URR_PASSWORD_C varchar(200) not null,
  URR_CREATEDATE_D timestamp not null,
  URR_STATUS_C varchar(10) not null,
  URR_PROCESSDATE_D timestamp,
  URR_NOTES_C varchar(500),
  primary key (URR_ID_C)
);

update T_CONFIG set CFG_VALUE_C = '32' where CFG_ID_C = 'DB_VERSION';