package com.bakouan.app.repositories;

import com.bakouan.app.model.BaCongeNotification;
import com.bakouan.app.enums.EStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface BaCongeNotificationRepository extends JpaRepository<BaCongeNotification,String> {
    List<BaCongeNotification> findByDestinataireIdAndStatutOrderByCreatedDateDesc(String destinataireId, EStatut statut);
    boolean existsByCle(String cle);
    @org.springframework.data.jpa.repository.Query(value="select * from ba_conge_notification where envoyee=false and essais < 10 and (prochain_essai is null or prochain_essai <= now()) order by created_date limit 20 for update skip locked",nativeQuery=true)
List<BaCongeNotification> findMailBatch();
}

