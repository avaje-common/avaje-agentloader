package misc.domain;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Entity1 {

  @Id
  Long id;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }
}
