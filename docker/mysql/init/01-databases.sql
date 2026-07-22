-- LinkCode 서비스별 데이터베이스 생성
--
-- 이 스크립트는 MySQL 컨테이너가 "빈 데이터 볼륨"으로 최초 기동될 때
-- /docker-entrypoint-initdb.d 에서 단 한 번 실행된다.
-- (mysql_data 볼륨이 이미 초기화된 뒤에는 다시 실행되지 않는다 —
--  DB를 다시 만들려면 `docker compose down -v` 로 볼륨을 지워야 한다)
--
-- compose의 MYSQL_DATABASE 는 DB를 하나만 만들 수 있어, 3개를 여기서 직접 만든다.
-- 앱은 root 계정으로 접속하므로(application.yml 기본값) 별도 GRANT는 필요 없다.

CREATE DATABASE IF NOT EXISTS linkcodeauth
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS linkcodeinterview
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS linkcodeconsumer
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
